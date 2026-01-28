package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.AttemptAnswerResponse;
import pl.co.assessment.dto.AttemptDetailResponse;
import pl.co.assessment.dto.AttemptListItemResponse;
import pl.co.assessment.dto.AttemptPageResponse;
import pl.co.assessment.dto.AttemptQuestionResponse;
import pl.co.assessment.dto.AttemptResultItemResponse;
import pl.co.assessment.dto.AttemptResultResponse;
import pl.co.assessment.entity.AttemptOptionOrder;
import pl.co.assessment.entity.AttemptQuestionOrder;
import pl.co.assessment.entity.ExamAttempt;
import pl.co.assessment.entity.ExamAttemptStatus;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.entity.UserAnswer;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.repository.AttemptOptionOrderRepository;
import pl.co.assessment.repository.AttemptQuestionOrderRepository;
import pl.co.assessment.repository.ExamAttemptRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.repository.UserAnswerRepository;
import pl.co.assessment.service.AttemptQueryService;
import pl.co.assessment.service.AttemptSubmissionService;
import pl.co.assessment.projection.AttemptListRow;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptQueryServiceImpl implements AttemptQueryService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamVersionQuestionRepository examVersionQuestionRepository;
    private final AttemptQuestionOrderRepository attemptQuestionOrderRepository;
    private final AttemptOptionOrderRepository attemptOptionOrderRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final AttemptSubmissionService attemptSubmissionService;

    @Override
    @Transactional
    public AttemptDetailResponse getAttempt(String attemptId, String userId) {
        ExamAttempt attempt = loadAttemptOrThrow(attemptId);
        assertOwner(attempt, userId);

        ExamVersion version = loadVersionOrThrow(attempt);
        List<ExamVersionQuestion> mappings = resolveQuestionOrder(attempt, version);
        Map<String, QuestionVersion> questionVersionMap = loadQuestionVersions(mappings);
        Map<String, List<String>> optionOrderMap = resolveOptionOrderMap(attempt, version);

        Long remainingSeconds = computeRemainingSeconds(attempt, version);
        boolean inProgress = ExamAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus());
        List<AttemptQuestionResponse> questions = inProgress
                ? buildQuestions(mappings, questionVersionMap, optionOrderMap)
                : null;
        List<AttemptAnswerResponse> answers = inProgress
                ? loadAnswers(attempt.getId())
                : null;

        return AttemptDetailResponse.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExamId())
                .examVersionId(version.getId())
                .status(attempt.getStatus())
                .name(version.getName())
                .description(version.getDescription())
                .durationMinutes(version.getDurationMinutes())
                .startTime(attempt.getStartTime())
                .timeRemainingSeconds(remainingSeconds)
                .questions(questions)
                .answers(answers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptPageResponse listAttempts(String userId,
                                            String status,
                                            String gradingStatus,
                                            Instant fromTime,
                                            Instant toTime,
                                            Integer page,
                                            Integer size) {
        int pageValue = page == null ? 0 : page;
        int sizeValue = size == null ? 20 : size;
        PageRequest pageRequest = PageRequest.of(Math.max(pageValue, 0), Math.max(sizeValue, 1));
        Page<AttemptListRow> result = examAttemptRepository.findAttemptList(
                userId,
                status,
                gradingStatus,
                fromTime,
                toTime,
                pageRequest
        );
        List<AttemptListItemResponse> items = result.getContent().stream()
                .map(row -> AttemptListItemResponse.builder()
                        .attemptId(row.getAttemptId())
                        .examId(row.getExamId())
                        .examVersionId(row.getExamVersionId())
                        .name(row.getName())
                        .description(row.getDescription())
                        .durationMinutes(row.getDurationMinutes())
                        .status(row.getStatus())
                        .gradingStatus(row.getGradingStatus())
                        .startTime(row.getStartTime())
                        .endTime(row.getEndTime())
                        .score(row.getScore())
                        .maxScore(row.getMaxScore())
                        .percent(row.getPercent())
                        .build())
                .toList();
        return AttemptPageResponse.builder()
                .items(items)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
    }

    @Override
    @Transactional
    public AttemptResultResponse getAttemptResult(String attemptId, String userId) {
        ExamAttempt attempt = loadAttemptOrThrow(attemptId);
        assertOwner(attempt, userId);

        if (ExamAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt is in progress"));
        }

        ExamVersion version = loadVersionOrThrow(attempt);
        List<ExamVersionQuestion> mappings = resolveQuestionOrder(attempt, version);
        Map<String, QuestionVersion> questionVersionMap = loadQuestionVersions(mappings);
        Map<String, List<String>> optionOrderMap = resolveOptionOrderMap(attempt, version);
        Map<String, UserAnswer> answerMap = loadAnswerMap(attempt.getId());

        List<AttemptResultItemResponse> items = buildResultItems(mappings, questionVersionMap, optionOrderMap, answerMap);

        return AttemptResultResponse.builder()
                .attemptId(attempt.getId())
                .examId(attempt.getExamId())
                .examVersionId(attempt.getExamVersionId())
                .status(attempt.getStatus())
                .gradingStatus(attempt.getGradingStatus())
                .name(version.getName())
                .description(version.getDescription())
                .durationMinutes(version.getDurationMinutes())
                .startTime(attempt.getStartTime())
                .endTime(attempt.getEndTime())
                .score(attempt.getScore())
                .maxScore(attempt.getMaxScore())
                .percent(attempt.getPercent())
                .items(items)
                .build();
    }

    private ExamAttempt loadAttemptOrThrow(String attemptId) {
        return examAttemptRepository.findByIdAndDeletedFalse(attemptId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Attempt not found")));
    }

    private void assertOwner(ExamAttempt attempt, String userId) {
        if (!userId.equals(attempt.getCreatedBy())) {
            throw new ApiException(ErrorCode.E230, ErrorCode.E230.message("No authority"));
        }
    }

    private ExamVersion loadVersionOrThrow(ExamAttempt attempt) {
        return examVersionRepository.findByIdAndDeletedFalse(attempt.getExamVersionId())
                .orElseThrow(() -> new ApiException(ErrorCode.E420, ErrorCode.E420.message("Exam version does not exist")));
    }

    private List<AttemptAnswerResponse> loadAnswers(String attemptId) {
        return userAnswerRepository.findByAttemptIdAndDeletedFalse(attemptId).stream()
                .map(this::toAnswerResponse)
                .toList();
    }

    private Map<String, UserAnswer> loadAnswerMap(String attemptId) {
        List<UserAnswer> answers = userAnswerRepository.findByAttemptIdAndDeletedFalse(attemptId);
        Map<String, UserAnswer> map = new HashMap<>();
        for (UserAnswer answer : answers) {
            map.put(answer.getExamVersionQuestionId(), answer);
        }
        return map;
    }

    private Long computeRemainingSeconds(ExamAttempt attempt, ExamVersion version) {
        // SUBMITTED or TIMEOUT => remaining = 0
        if (ExamAttemptStatus.SUBMITTED.name().equalsIgnoreCase(attempt.getStatus())
                || ExamAttemptStatus.TIMEOUT.name().equalsIgnoreCase(attempt.getStatus())) {
            return 0L;
        }
        if (version.getDurationMinutes() == null || attempt.getStartTime() == null) {
            return null;
        }
        long durationSeconds = version.getDurationMinutes() * 60L;
        if (durationSeconds <= 0L) {
            return null;
        }
        long elapsed = Duration.between(attempt.getStartTime(), Instant.now()).getSeconds();
        long remaining = durationSeconds - elapsed;
        if (remaining <= 0L) {
            attemptSubmissionService.timeout(List.of(attempt));
            return 0L;
        }
        return remaining;
    }

    private List<ExamVersionQuestion> resolveQuestionOrder(ExamAttempt attempt, ExamVersion version) {
        if (Boolean.TRUE.equals(version.isShuffleQuestions())) {
            // Use stored attempt question order
            List<AttemptQuestionOrder> orders =
                    attemptQuestionOrderRepository.findByAttemptIdAndDeletedFalseOrderByDisplayOrderAsc(attempt.getId());
            if (orders.isEmpty()) {
                // Fallback to default order if order rows missing
                return examVersionQuestionRepository
                        .findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(version.getId());
            }
            List<String> examVersionQuestionIds = orders.stream()
                    .map(AttemptQuestionOrder::getExamVersionQuestionId)
                    .toList();
            Map<String, ExamVersionQuestion> mappingMap = examVersionQuestionRepository
                    .findByIdInAndDeletedFalse(examVersionQuestionIds)
                    .stream()
                    .collect(Collectors.toMap(ExamVersionQuestion::getId, item -> item));
            List<ExamVersionQuestion> ordered = new ArrayList<>();
            for (AttemptQuestionOrder order : orders) {
                ExamVersionQuestion mapping = mappingMap.get(order.getExamVersionQuestionId());
                if (mapping != null) {
                    ordered.add(mapping);
                }
            }
            return ordered;
        }
        // Default order from exam version
        return examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(version.getId());
    }

    private Map<String, QuestionVersion> loadQuestionVersions(List<ExamVersionQuestion> mappings) {
        List<String> questionVersionIds = mappings.stream()
                .map(ExamVersionQuestion::getQuestionVersionId)
                .distinct()
                .toList();
        if (questionVersionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuestionVersion> questionVersions = questionVersionRepository.findByIdInAndDeletedFalse(questionVersionIds);
        Map<String, QuestionVersion> questionVersionMap = new HashMap<>();
        for (QuestionVersion qv : questionVersions) {
            questionVersionMap.put(qv.getId(), qv);
        }
        return questionVersionMap;
    }

    private Map<String, List<String>> resolveOptionOrderMap(ExamAttempt attempt, ExamVersion version) {
        if (!Boolean.TRUE.equals(version.isShuffleOptions())) {
            return Collections.emptyMap();
        }
        // Option order stored per attempt + question_version_id
        List<AttemptOptionOrder> orders =
                attemptOptionOrderRepository.findByAttemptIdAndDeletedFalseOrderByQuestionVersionIdAscDisplayOrderAsc(attempt.getId());
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> optionOrderMap = new HashMap<>();
        for (AttemptOptionOrder order : orders) {
            optionOrderMap
                    .computeIfAbsent(order.getQuestionVersionId(), key -> new ArrayList<>())
                    .add(order.getOptionKey());
        }
        return optionOrderMap;
    }

    private List<AttemptQuestionResponse> buildQuestions(List<ExamVersionQuestion> mappings,
                                                         Map<String, QuestionVersion> questionVersionMap,
                                                         Map<String, List<String>> optionOrderMap) {
        List<AttemptQuestionResponse> questions = new ArrayList<>();
        int displayOrder = 1;
        for (ExamVersionQuestion mapping : mappings) {
            QuestionVersion qv = questionVersionMap.get(mapping.getQuestionVersionId());
            if (qv == null) {
                continue;
            }
            // Build question content; reorder options if option order exists
            QuestionContent content = qv.getQuestionContent();
            QuestionContent resolvedContent = applyOptionOrder(content, qv.getId(), optionOrderMap);
            questions.add(AttemptQuestionResponse.builder()
                    .order(displayOrder++)
                    .examVersionQuestionId(mapping.getId())
                    .questionVersionId(qv.getId())
                    .type(qv.getType())
                    .questionContent(resolvedContent)
                    .build());
        }
        return questions;
    }

    private List<AttemptResultItemResponse> buildResultItems(List<ExamVersionQuestion> mappings,
                                                             Map<String, QuestionVersion> questionVersionMap,
                                                             Map<String, List<String>> optionOrderMap,
                                                             Map<String, UserAnswer> answerMap) {
        List<AttemptResultItemResponse> items = new ArrayList<>();
        int displayOrder = 1;
        for (ExamVersionQuestion mapping : mappings) {
            QuestionVersion qv = questionVersionMap.get(mapping.getQuestionVersionId());
            if (qv == null) {
                continue;
            }
            QuestionContent content = qv.getQuestionContent();
            QuestionContent resolvedContent = applyOptionOrder(content, qv.getId(), optionOrderMap);
            UserAnswer answer = answerMap.get(mapping.getId());
            items.add(AttemptResultItemResponse.builder()
                    .order(displayOrder++)
                    .examVersionQuestionId(mapping.getId())
                    .questionVersionId(qv.getId())
                    .type(qv.getType())
                    .questionContent(resolvedContent)
                    .gradingRules(qv.getGradingRules())
                    .answerJson(answer == null ? null : answer.getAnswerJson())
                    .earnedPoints(answer == null ? null : answer.getEarnedPoints())
                    .answerGradingStatus(answer == null ? null : answer.getGradingStatus())
                    .build());
        }
        return items;
    }

    private QuestionContent applyOptionOrder(QuestionContent content,
                                             String questionVersionId,
                                             Map<String, List<String>> optionOrderMap) {
        if (content == null || content.getOptions() == null || content.getOptions().isEmpty()) {
            return content;
        }
        List<String> order = optionOrderMap.get(questionVersionId);
        if (order == null || order.isEmpty()) {
            return content;
        }
        Map<String, QuestionContent.Option> optionMap = new HashMap<>();
        for (QuestionContent.Option option : content.getOptions()) {
            optionMap.put(option.getId(), option);
        }
        List<QuestionContent.Option> sorted = new ArrayList<>();
        for (String key : order) {
            QuestionContent.Option option = optionMap.get(key);
            if (option != null) {
                sorted.add(option);
            }
        }
        if (sorted.isEmpty()) {
            return content;
        }
        // Rebuild content with reordered options to avoid mutating original reference
        return QuestionContent.builder()
                .schemaVersion(content.getSchemaVersion())
                .prompt(content.getPrompt())
                .explanation(content.getExplanation())
                .options(sorted)
                .matching(content.getMatching())
                .blanks(content.getBlanks())
                .fileUpload(content.getFileUpload())
                .build();
    }

    private AttemptAnswerResponse toAnswerResponse(UserAnswer answer) {
        return AttemptAnswerResponse.builder()
                .examVersionQuestionId(answer.getExamVersionQuestionId())
                .answerJson(answer.getAnswerJson())
                .build();
    }
}
