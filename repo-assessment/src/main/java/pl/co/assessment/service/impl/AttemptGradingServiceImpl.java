package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.entity.ExamAttempt;
import pl.co.assessment.entity.ExamAttemptGradingStatus;
import pl.co.assessment.entity.ExamAttemptStatus;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.QuestionType;
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.entity.UserAnswer;
import pl.co.assessment.entity.UserAnswerGradingStatus;
import pl.co.assessment.entity.json.AnswerJson;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.repository.ExamAttemptRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.repository.UserAnswerRepository;
import pl.co.assessment.service.AttemptGradingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttemptGradingServiceImpl implements AttemptGradingService {

    private static final int SCORE_SCALE = 2;

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamVersionQuestionRepository examVersionQuestionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final UserAnswerRepository userAnswerRepository;

    @Override
    @Transactional
    public void gradeAttempt(String attemptId) {
        // 1) Guard: skip invalid input or non-gradable attempts
        if (attemptId == null || attemptId.isBlank()) {
            return;
        }
        ExamAttempt attempt = examAttemptRepository.findByIdAndDeletedFalseForUpdate(attemptId)
                .orElse(null);
        if (attempt == null) {
            return;
        }
        String status = attempt.getStatus();
        String gradingStatus = attempt.getGradingStatus();
        if (ExamAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(status)) {
            return;
        }
        if (!ExamAttemptGradingStatus.AUTO_GRADING.name().equalsIgnoreCase(gradingStatus)) {
            return;
        }

        // 2) Load exam version + question mapping
        ExamVersion version = examVersionRepository.findByIdAndDeletedFalse(attempt.getExamVersionId())
                .orElse(null);
        if (version == null) {
            return;
        }

        List<ExamVersionQuestion> mappings =
                examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(version.getId());
        if (mappings.isEmpty()) {
            // No questions -> auto finalize with zero score
            attempt.setScore(BigDecimal.ZERO);
            attempt.setMaxScore(BigDecimal.ZERO);
            attempt.setGradingStatus(ExamAttemptGradingStatus.GRADED.name());
            examAttemptRepository.save(attempt);
            return;
        }

        // 3) Batch load question versions + user answers
        Map<String, QuestionVersion> questionVersions = loadQuestionVersions(mappings);
        Map<String, UserAnswer> answers = loadUserAnswers(attemptId);
        Instant gradedAt = Instant.now();

        // 4) Grade per question and accumulate totals
        BigDecimal totalMax = BigDecimal.ZERO;
        BigDecimal totalScore = BigDecimal.ZERO;
        boolean manualRequired = false;
        boolean autoPending = false;

        for (ExamVersionQuestion mapping : mappings) {
            QuestionVersion questionVersion = questionVersions.get(mapping.getQuestionVersionId());
            if (questionVersion == null) {
                continue;
            }
            GradingRules rules = questionVersion.getGradingRules();
            BigDecimal maxPoints = rules == null || rules.getMaxPoints() == null
                    ? BigDecimal.ZERO
                    : rules.getMaxPoints();
            totalMax = totalMax.add(maxPoints);

            UserAnswer answer = answers.get(mapping.getId());
            AnswerJson answerJson = answer == null ? null : answer.getAnswerJson();
            QuestionType type = resolveQuestionType(questionVersion.getType());
            if (type == null) {
                continue;
            }

            switch (type) {
                case SINGLE_CHOICE, MULTIPLE_CHOICE, SHORT_TEXT, MATCHING, FILL_BLANKS -> {
                    // Auto-gradable types
                    BigDecimal earned = gradeAuto(type, answerJson, rules, maxPoints);
                    totalScore = totalScore.add(earned);
                    if (answer != null) {
                        answer.setEarnedPoints(earned);
                        answer.setGradingStatus(UserAnswerGradingStatus.FINALIZED.name());
                        answer.setGradedAt(gradedAt);
                        answer.setGraderId(null);
                    }
                }
                case ESSAY, FILE_UPLOAD -> {
                    // Manual types by default; optional auto_mode is TODO
                    GradingRules.Manual manual = rules == null ? null : rules.getManual();
                    boolean autoMode = manual != null && Boolean.TRUE.equals(manual.getAutoMode());
                    if (autoMode) {
                        // TODO: auto grading for ESSAY/FILE_UPLOAD when auto_mode = true
                        autoPending = true;
                    } else {
                        manualRequired = true;
                        if (answer != null) {
                            answer.setEarnedPoints(null);
                            answer.setGradingStatus(UserAnswerGradingStatus.MANUAL_PENDING.name());
                            answer.setGradedAt(null);
                            answer.setGraderId(null);
                        }
                    }
                }
                default -> {
                    // ignore unsupported types
                }
            }
        }

        // 5) Persist answer grading results
        if (!answers.isEmpty()) {
            userAnswerRepository.saveAll(answers.values());
        }

        // 6) Persist attempt totals and grading status
        attempt.setScore(totalScore);
        attempt.setMaxScore(totalMax);
        attempt.setPercent(calculatePercent(totalScore, totalMax));
        if (manualRequired) {
            attempt.setGradingStatus(ExamAttemptGradingStatus.MANUAL_GRADING.name());
        } else if (autoPending) {
            attempt.setGradingStatus(ExamAttemptGradingStatus.AUTO_GRADING.name());
        } else {
            attempt.setGradingStatus(ExamAttemptGradingStatus.GRADED.name());
        }
        examAttemptRepository.save(attempt);
    }

    private Map<String, QuestionVersion> loadQuestionVersions(List<ExamVersionQuestion> mappings) {
        List<String> ids = mappings.stream()
                .map(ExamVersionQuestion::getQuestionVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<QuestionVersion> versions = questionVersionRepository.findByIdInAndDeletedFalse(ids);
        Map<String, QuestionVersion> result = new HashMap<>();
        for (QuestionVersion version : versions) {
            result.put(version.getId(), version);
        }
        return result;
    }

    private Map<String, UserAnswer> loadUserAnswers(String attemptId) {
        List<UserAnswer> answers = userAnswerRepository.findByAttemptIdAndDeletedFalse(attemptId);
        Map<String, UserAnswer> map = new HashMap<>();
        for (UserAnswer answer : answers) {
            map.put(answer.getExamVersionQuestionId(), answer);
        }
        return map;
    }

    private BigDecimal gradeAuto(QuestionType type,
                                 AnswerJson answerJson,
                                 GradingRules rules,
                                 BigDecimal maxPoints) {
        if (answerJson == null || answerJson.getPayload() == null) {
            return BigDecimal.ZERO;
        }
        AnswerJson.Payload payload = answerJson.getPayload();
        return switch (type) {
            case SINGLE_CHOICE -> gradeChoice(payload, rules, maxPoints);
            case MULTIPLE_CHOICE -> gradeChoice(payload, rules, maxPoints);
            case SHORT_TEXT -> gradeShortText(payload, rules, maxPoints);
            case MATCHING -> gradeMatching(payload, rules, maxPoints);
            case FILL_BLANKS -> gradeFillBlanks(payload, rules, maxPoints);
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal gradeChoice(AnswerJson.Payload payload, GradingRules rules, BigDecimal maxPoints) {
        if (rules == null || rules.getChoice() == null || rules.getChoice().getCorrectOptionIds() == null) {
            return BigDecimal.ZERO;
        }
        List<String> selected = payload.getSelectedOptionIds();
        if (selected == null || selected.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<String> selectedSet = new HashSet<>(selected);
        Set<String> correctSet = new HashSet<>(rules.getChoice().getCorrectOptionIds());
        return selectedSet.equals(correctSet) ? maxPoints : BigDecimal.ZERO;
    }

    private BigDecimal gradeShortText(AnswerJson.Payload payload, GradingRules rules, BigDecimal maxPoints) {
        if (rules == null || rules.getShortText() == null || rules.getShortText().getAccepted() == null) {
            return BigDecimal.ZERO;
        }
        String answer = normalizeText(payload.getText());
        if (answer == null || answer.isBlank()) {
            return BigDecimal.ZERO;
        }
        String matchMethod = rules.getShortText().getMatchMethod();
        for (String acceptedRaw : rules.getShortText().getAccepted()) {
            String accepted = normalizeText(acceptedRaw);
            if (accepted == null || accepted.isBlank()) {
                continue;
            }
            if (isTextMatch(answer, accepted, matchMethod)) {
                return maxPoints;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal gradeMatching(AnswerJson.Payload payload, GradingRules rules, BigDecimal maxPoints) {
        if (rules == null || rules.getMatching() == null || rules.getMatching().getPairs() == null) {
            return BigDecimal.ZERO;
        }
        List<AnswerJson.Pair> pairs = payload.getPairs();
        if (pairs == null || pairs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<String, String> correct = new HashMap<>();
        for (GradingRules.Pair pair : rules.getMatching().getPairs()) {
            correct.put(pair.getLeftId(), pair.getRightId());
        }
        String scheme = normalizeScheme(rules.getMatching().getScheme());
        int totalPairs = correct.size();
        if (totalPairs == 0) {
            return BigDecimal.ZERO;
        }
        int correctCount = 0;
        for (AnswerJson.Pair pair : pairs) {
            if (pair == null || pair.getLeftId() == null || pair.getRightId() == null) {
                continue;
            }
            String expected = correct.get(pair.getLeftId());
            if (expected != null && expected.equals(pair.getRightId())) {
                correctCount++;
            }
        }
        if ("all_or_nothing".equals(scheme)) {
            return correctCount == totalPairs ? maxPoints : BigDecimal.ZERO;
        }
        BigDecimal perPair = maxPoints.divide(BigDecimal.valueOf(totalPairs), SCORE_SCALE, RoundingMode.HALF_UP);
        return perPair.multiply(BigDecimal.valueOf(correctCount));
    }

    private BigDecimal gradeFillBlanks(AnswerJson.Payload payload,
                                       GradingRules rules,
                                       BigDecimal maxPoints) {
        if (rules == null || rules.getFillBlanks() == null || rules.getFillBlanks().getBlanks() == null) {
            return BigDecimal.ZERO;
        }
        List<AnswerJson.BlankAnswer> blanks = payload.getBlanks();
        if (blanks == null || blanks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<String, GradingRules.BlankRule> ruleMap = new HashMap<>();
        for (GradingRules.BlankRule rule : rules.getFillBlanks().getBlanks()) {
            ruleMap.put(rule.getBlankId(), rule);
        }
        if (ruleMap.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String inputKind = rules.getFillBlanks().getInputKind();
        String scheme = normalizeScheme(rules.getFillBlanks().getScheme());
        int totalBlanks = ruleMap.size();
        int correctCount = 0;
        for (AnswerJson.BlankAnswer blank : blanks) {
            if (blank == null || blank.getBlankId() == null) {
                continue;
            }
            GradingRules.BlankRule rule = ruleMap.get(blank.getBlankId());
            if (rule == null) {
                continue;
            }
            if (isBlankCorrect(blank, rule, inputKind)) {
                correctCount++;
            }
        }
        if ("all_or_nothing".equals(scheme)) {
            return correctCount == totalBlanks ? maxPoints : BigDecimal.ZERO;
        }
        BigDecimal perBlank = maxPoints.divide(BigDecimal.valueOf(totalBlanks), SCORE_SCALE, RoundingMode.HALF_UP);
        return perBlank.multiply(BigDecimal.valueOf(correctCount));
    }

    private boolean isBlankCorrect(AnswerJson.BlankAnswer blank,
                                   GradingRules.BlankRule rule,
                                   String inputKind) {
        if (inputKind != null && (inputKind.equalsIgnoreCase("word_bank") || inputKind.equalsIgnoreCase("select"))) {
            List<String> selected = blank.getSelectedOptionIds();
            List<String> correct = rule.getCorrectOptionIds();
            if (selected == null || correct == null) {
                return false;
            }
            return new HashSet<>(selected).equals(new HashSet<>(correct));
        }
        String answer = normalizeText(blank.getValue());
        if (answer == null || answer.isBlank()) {
            return false;
        }
        List<String> accepted = rule.getAccepted();
        if (accepted == null || accepted.isEmpty()) {
            return false;
        }
        String matchMethod = rule.getMatchMethod();
        for (String acceptedRaw : accepted) {
            String candidate = normalizeText(acceptedRaw);
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (isTextMatch(answer, candidate, matchMethod)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeScheme(String scheme) {
        if (scheme == null || scheme.isBlank()) {
            return "per_pair";
        }
        return scheme.trim().toLowerCase(Locale.ROOT);
    }

    private QuestionType resolveQuestionType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        try {
            return QuestionType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isTextMatch(String answer, String accepted, String matchMethod) {
        if (matchMethod == null || matchMethod.isBlank()) {
            return answer.equals(accepted);
        }
        String normalized = matchMethod.trim().toLowerCase(Locale.ROOT);
        if ("contains".equals(normalized)) {
            return answer.contains(accepted);
        }
        return answer.equals(accepted);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", " ");
    }

    private BigDecimal calculatePercent(BigDecimal score, BigDecimal maxScore) {
        if (score == null || maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return score
                .multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 2, RoundingMode.HALF_UP);
    }
}
