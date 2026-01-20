package pl.co.assessment.service.impl;

import org.springframework.stereotype.Service;
import pl.co.assessment.dto.ExamDraftChangeRequest;
import pl.co.assessment.entity.QuestionType;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.service.QuestionService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class QuestionServiceImpl implements QuestionService {

    private static final int SCHEMA_VERSION = 1;

    @Override
    public String normalizeQuestionType(String type, String questionId) {
        // Validate raw type input and map to supported enum values.
        if (type == null || type.isBlank()) {
            throw new ApiException(ErrorCode.E221, "Invalid question type, questionId: " + questionId);
        }
        String normalized = type.toUpperCase(Locale.ROOT);
        try {
            return QuestionType.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.E221, "Invalid question type, questionId: " + questionId);
        }
    }

    @Override
    public void processQuestionPayload(ExamDraftChangeRequest change, String normalizedType) {
        // 1) Schema version + prune fields not used by the selected type.
        QuestionContent content = change.getQuestionContent();
        if (content != null) {
            content.setSchemaVersion(SCHEMA_VERSION);
            normalizeQuestionContentByType(content, normalizedType);
        }
        GradingRules rules = change.getGradingRules();
        if (rules != null) {
            rules.setSchemaVersion(SCHEMA_VERSION);
            normalizeGradingRulesByType(rules, normalizedType);
        }

        // 2) Validate required fields for the selected type.
        validateQuestionPayloadByType(change, normalizedType);

        // 3) Normalize fill-blank mode specifics (ignore irrelevant fields).
        normalizeFillBlankInputs(change, normalizedType);

        // 4) Manual grading rules for ESSAY/FILE_UPLOAD.
        normalizeManualRules(change, normalizedType);
    }

    private void normalizeQuestionContentByType(QuestionContent content, String normalizedType) {
        if (QuestionType.SINGLE_CHOICE.name().equalsIgnoreCase(normalizedType)
                || QuestionType.MULTIPLE_CHOICE.name().equalsIgnoreCase(normalizedType)) {
            content.setMatching(null);
            content.setBlanks(null);
            content.setFileUpload(null);
            return;
        }
        if (QuestionType.SHORT_TEXT.name().equalsIgnoreCase(normalizedType)) {
            content.setOptions(null);
            content.setMatching(null);
            content.setBlanks(null);
            content.setFileUpload(null);
            return;
        }
        if (QuestionType.MATCHING.name().equalsIgnoreCase(normalizedType)) {
            content.setOptions(null);
            content.setBlanks(null);
            content.setFileUpload(null);
            return;
        }
        if (QuestionType.FILL_BLANKS.name().equalsIgnoreCase(normalizedType)) {
            content.setOptions(null);
            content.setMatching(null);
            content.setFileUpload(null);
            return;
        }
        if (QuestionType.FILE_UPLOAD.name().equalsIgnoreCase(normalizedType)) {
            content.setOptions(null);
            content.setMatching(null);
            content.setBlanks(null);
            return;
        }
        if (QuestionType.ESSAY.name().equalsIgnoreCase(normalizedType)) {
            content.setOptions(null);
            content.setMatching(null);
            content.setBlanks(null);
            content.setFileUpload(null);
        }
    }

    private void normalizeGradingRulesByType(GradingRules rules, String normalizedType) {
        if (QuestionType.SINGLE_CHOICE.name().equalsIgnoreCase(normalizedType)
                || QuestionType.MULTIPLE_CHOICE.name().equalsIgnoreCase(normalizedType)) {
            rules.setShortText(null);
            rules.setMatching(null);
            rules.setFillBlanks(null);
            rules.setManual(null);
            return;
        }
        if (QuestionType.SHORT_TEXT.name().equalsIgnoreCase(normalizedType)) {
            rules.setChoice(null);
            rules.setMatching(null);
            rules.setFillBlanks(null);
            rules.setManual(null);
            return;
        }
        if (QuestionType.MATCHING.name().equalsIgnoreCase(normalizedType)) {
            rules.setChoice(null);
            rules.setShortText(null);
            rules.setFillBlanks(null);
            rules.setManual(null);
            return;
        }
        if (QuestionType.FILL_BLANKS.name().equalsIgnoreCase(normalizedType)) {
            rules.setChoice(null);
            rules.setShortText(null);
            rules.setMatching(null);
            rules.setManual(null);
            return;
        }
        if (QuestionType.FILE_UPLOAD.name().equalsIgnoreCase(normalizedType)
                || QuestionType.ESSAY.name().equalsIgnoreCase(normalizedType)) {
            rules.setChoice(null);
            rules.setShortText(null);
            rules.setMatching(null);
            rules.setFillBlanks(null);
        }
    }

    private void validateQuestionPayloadByType(ExamDraftChangeRequest change, String normalizedType) {
        String questionId = change.getQuestionId();
        QuestionContent content = change.getQuestionContent();
        GradingRules rules = change.getGradingRules();
        if (rules != null && rules.getMaxPoints() != null && rules.getMaxPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.E204,
                    ErrorCode.E204.message("gradingRules.maxPoints must be > 0, questionId: " + questionId));
        }

        if (QuestionType.SINGLE_CHOICE.name().equalsIgnoreCase(normalizedType)
                || QuestionType.MULTIPLE_CHOICE.name().equalsIgnoreCase(normalizedType)) {
            if (content == null || content.getOptions() == null || content.getOptions().isEmpty()) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("questionContent.options is required, questionId: " + questionId));
            }
            if (rules == null || rules.getChoice() == null) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.choice is required, questionId: " + questionId));
            }
            if (rules.getChoice().getCorrectOptionIds() == null || rules.getChoice().getCorrectOptionIds().isEmpty()) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.choice.correctOptionIds is required, questionId: " + questionId));
            }
            return;
        }

        if (QuestionType.SHORT_TEXT.name().equalsIgnoreCase(normalizedType)) {
            if (rules == null || rules.getShortText() == null) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.shortText is required, questionId: " + questionId));
            }
            if (rules.getShortText().getAccepted() == null || rules.getShortText().getAccepted().isEmpty()) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.shortText.accepted is required, questionId: " + questionId));
            }
            if (!isMatchMethodValid(rules.getShortText().getMatchMethod())) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.shortText.matchMethod is invalid, questionId: " + questionId));
            }
            return;
        }

        if (QuestionType.MATCHING.name().equalsIgnoreCase(normalizedType)) {
            if (content == null || content.getMatching() == null) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("questionContent.matching is required, questionId: " + questionId));
            }
            if (rules == null || rules.getMatching() == null) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.matching is required, questionId: " + questionId));
            }
            if (rules.getMatching().getPairs() == null || rules.getMatching().getPairs().isEmpty()) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.matching.pairs is required, questionId: " + questionId));
            }
            if (!isSchemeValid(rules.getMatching().getScheme())) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.matching.scheme is invalid, questionId: " + questionId));
            }
            return;
        }

        if (QuestionType.FILL_BLANKS.name().equalsIgnoreCase(normalizedType)) {
            if (content == null || content.getBlanks() == null) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("questionContent.blanks is required, questionId: " + questionId));
            }
            if (!isInputKindValid(content.getBlanks().getInputKind())) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("questionContent.blanks.inputKind is invalid, questionId: " + questionId));
            }
            boolean isSelect = isSelectInput(content.getBlanks().getInputKind());
            if (isSelect) {
                if (content.getBlanks().getWordBank() == null || content.getBlanks().getWordBank().isEmpty()) {
                    throw new ApiException(ErrorCode.E204,
                            ErrorCode.E204.message("questionContent.blanks.wordBank is required, questionId: " + questionId));
                }
            }
            if (rules == null || rules.getFillBlanks() == null) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.fillBlanks is required, questionId: " + questionId));
            }
            if (rules.getFillBlanks().getBlanks() == null || rules.getFillBlanks().getBlanks().isEmpty()) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.fillBlanks.blanks is required, questionId: " + questionId));
            }
            if (!isSchemeValid(rules.getFillBlanks().getScheme())) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("gradingRules.fillBlanks.scheme is invalid, questionId: " + questionId));
            }
            for (GradingRules.BlankRule blank : rules.getFillBlanks().getBlanks()) {
                if (blank == null) {
                    continue;
                }
                if (isSelect) {
                    if (blank.getCorrectOptionIds() == null || blank.getCorrectOptionIds().isEmpty()) {
                        throw new ApiException(ErrorCode.E204,
                                ErrorCode.E204.message("gradingRules.fillBlanks.correctOptionIds is required, questionId: " + questionId));
                    }
                } else {
                    if (blank.getAccepted() == null || blank.getAccepted().isEmpty()) {
                        throw new ApiException(ErrorCode.E204,
                                ErrorCode.E204.message("gradingRules.fillBlanks.accepted is required, questionId: " + questionId));
                    }
                    if (!isMatchMethodValid(blank.getMatchMethod())) {
                        throw new ApiException(ErrorCode.E204,
                                ErrorCode.E204.message("gradingRules.fillBlanks.matchMethod is invalid, questionId: " + questionId));
                    }
                }
            }
            return;
        }

        if (QuestionType.FILE_UPLOAD.name().equalsIgnoreCase(normalizedType)) {
            if (content == null || content.getFileUpload() == null) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("questionContent.fileUpload is required, questionId: " + questionId));
            }
            Integer maxFiles = content.getFileUpload().getMaxFiles();
            if (maxFiles == null || maxFiles <= 0) {
                throw new ApiException(ErrorCode.E204,
                        ErrorCode.E204.message("questionContent.fileUpload.maxFiles must be > 0, questionId: " + questionId));
            }
        }
    }

    private void normalizeFillBlankInputs(ExamDraftChangeRequest change, String normalizedType) {
        if (!QuestionType.FILL_BLANKS.name().equalsIgnoreCase(normalizedType)) {
            return;
        }
        QuestionContent content = change.getQuestionContent();
        GradingRules rules = change.getGradingRules();
        if (content == null || content.getBlanks() == null || rules == null || rules.getFillBlanks() == null) {
            return;
        }
        boolean isSelect = isSelectInput(content.getBlanks().getInputKind());
        if (!isSelect && content.getBlanks().getWordBank() != null && !content.getBlanks().getWordBank().isEmpty()) {
            content.getBlanks().setWordBank(null);
        }
        List<GradingRules.BlankRule> blanks = rules.getFillBlanks().getBlanks();
        if (blanks == null || blanks.isEmpty()) {
            return;
        }
        for (GradingRules.BlankRule blank : blanks) {
            if (blank == null) {
                continue;
            }
            if (isSelect) {
                if (blank.getAccepted() != null && !blank.getAccepted().isEmpty()) {
                    blank.setAccepted(null);
                }
                if (blank.getMatchMethod() != null && !blank.getMatchMethod().isBlank()) {
                    blank.setMatchMethod(null);
                }
            } else if (blank.getCorrectOptionIds() != null && !blank.getCorrectOptionIds().isEmpty()) {
                blank.setCorrectOptionIds(null);
            }
        }
    }

    private void normalizeManualRules(ExamDraftChangeRequest change, String normalizedType) {
        GradingRules rules = change.getGradingRules();
        if (rules == null) {
            return;
        }
        if (!QuestionType.ESSAY.name().equalsIgnoreCase(normalizedType)
                && !QuestionType.FILE_UPLOAD.name().equalsIgnoreCase(normalizedType)) {
            rules.setManual(null);
            return;
        }
        GradingRules.Manual manual = rules.getManual();
        if (manual == null) {
            return;
        }
        if (manual.getAutoMode() == null) {
            manual.setAutoMode(Boolean.FALSE);
        }
        List<GradingRules.RubricItem> rubric = manual.getRubric();
        if (rubric == null || rubric.isEmpty()) {
            return;
        }
        BigDecimal maxPoints = rules.getMaxPoints();
        if (maxPoints == null) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (GradingRules.RubricItem item : rubric) {
            if (item != null && item.getMaxPoints() != null) {
                total = total.add(item.getMaxPoints());
            }
        }
        if (total.compareTo(maxPoints) > 0) {
            throw new ApiException(ErrorCode.E204,
                    ErrorCode.E204.message("Manual rubric maxPoints exceeds gradingRules.maxPoints, questionId: " + change.getQuestionId()));
        }
    }

    private boolean isSchemeValid(String scheme) {
        if (scheme == null || scheme.isBlank()) {
            return false;
        }
        String normalized = scheme.toLowerCase(Locale.ROOT);
        return "per_pair".equals(normalized) || "all_or_nothing".equals(normalized);
    }

    private boolean isMatchMethodValid(String method) {
        if (method == null || method.isBlank()) {
            return false;
        }
        String normalized = method.toLowerCase(Locale.ROOT);
        return "exact".equals(normalized) || "contains".equals(normalized);
    }

    private boolean isInputKindValid(String inputKind) {
        if (inputKind == null || inputKind.isBlank()) {
            return false;
        }
        String normalized = inputKind.toLowerCase(Locale.ROOT);
        return "text".equals(normalized) || "select".equals(normalized);
    }

    private boolean isSelectInput(String inputKind) {
        return "select".equalsIgnoreCase(inputKind);
    }
}
