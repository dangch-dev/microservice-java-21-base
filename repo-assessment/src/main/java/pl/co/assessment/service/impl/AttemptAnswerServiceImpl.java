package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.AttemptAnswerSaveItem;
import pl.co.assessment.dto.AttemptAnswerSaveRequest;
import pl.co.assessment.entity.ExamAttempt;
import pl.co.assessment.entity.ExamAttemptStatus;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.QuestionType;
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.entity.UserAnswer;
import pl.co.assessment.entity.UserAnswerGradingStatus;
import pl.co.assessment.entity.json.AnswerJson;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.repository.ExamAttemptRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.repository.UserAnswerRepository;
import pl.co.assessment.service.AttemptAnswerService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttemptAnswerServiceImpl implements AttemptAnswerService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamVersionQuestionRepository examVersionQuestionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final UserAnswerRepository userAnswerRepository;

    @Override
    @Transactional
    public void saveAnswers(String attemptId, String userId, AttemptAnswerSaveRequest request) {
        // Fast exit when request has no answers
        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return;
        }

        // Lock attempt and validate ownership/status
        ExamAttempt attempt = examAttemptRepository.findByIdAndDeletedFalseForUpdate(attemptId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Attempt not found")));

        // ensure Owner
        if (!userId.equals(attempt.getCreatedBy())) {
            throw new ApiException(ErrorCode.E230, ErrorCode.E230.message("No authority"));
        }
        //ensure Writable
        String status = attempt.getStatus();
        if (ExamAttemptStatus.SUBMITTED.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt is already submitted"));
        }
        if (ExamAttemptStatus.TIMEOUT.name().equalsIgnoreCase(status)) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt expired"));
        }

        // Resolve only valid examVersionQuestionIds for this attempt (ignore invalid ones)
        List<String> questionIds = request.getAnswers().stream()
                .map(AttemptAnswerSaveItem::getExamVersionQuestionId)
                .distinct()
                .toList();
        Map<String, ExamVersionQuestion> validMappings = loadValidExamVersionQuestions(attempt.getExamVersionId(), questionIds);
        if (validMappings.isEmpty()) {
            return;
        }

        // Validate answerJson against question content + grading rules (fail-all)
        Map<String, QuestionVersion> questionVersionMap = loadQuestionVersionsForValidation(request.getAnswers(), validMappings);
        validateAnswers(request.getAnswers(), validMappings, questionVersionMap);

        // Build upsert list (insert/update/soft-delete) and persist in batch
        List<UserAnswer> toSave = buildUpserts(attempt.getId(), request.getAnswers(), validMappings);

        if (!toSave.isEmpty()) {
            userAnswerRepository.saveAll(toSave);
        }
    }

    private Map<String, ExamVersionQuestion> loadValidExamVersionQuestions(String examVersionId, List<String> examVersionQuestionIds) {
        if (examVersionQuestionIds.isEmpty()) {
            return Map.of();
        }
        List<ExamVersionQuestion> mappings = examVersionQuestionRepository
                .findByExamVersionIdAndIdInAndDeletedFalse(examVersionId, examVersionQuestionIds);
        Map<String, ExamVersionQuestion> valid = new HashMap<>();
        for (ExamVersionQuestion mapping : mappings) {
            valid.put(mapping.getId(), mapping);
        }
        return valid;
    }

    private Map<String, QuestionVersion> loadQuestionVersionsForValidation(List<AttemptAnswerSaveItem> items,
                                                                           Map<String, ExamVersionQuestion> mappings) {
        List<String> questionVersionIds = items.stream()
                .filter(item -> item.getAnswerJson() != null)
                .map(AttemptAnswerSaveItem::getExamVersionQuestionId)
                .map(mappings::get)
                .filter(mapping -> mapping != null)
                .map(ExamVersionQuestion::getQuestionVersionId)
                .distinct()
                .toList();
        if (questionVersionIds.isEmpty()) {
            return Map.of();
        }
        List<QuestionVersion> questionVersions = questionVersionRepository.findByIdInAndDeletedFalse(questionVersionIds);
        Map<String, QuestionVersion> map = new HashMap<>();
        for (QuestionVersion questionVersion : questionVersions) {
            map.put(questionVersion.getId(), questionVersion);
        }
        return map;
    }

    private void validateAnswers(List<AttemptAnswerSaveItem> items,
                                 Map<String, ExamVersionQuestion> mappings,
                                 Map<String, QuestionVersion> questionVersions) {
        for (AttemptAnswerSaveItem item : items) {
            ExamVersionQuestion mapping = mappings.get(item.getExamVersionQuestionId());
            if (mapping == null || item.getAnswerJson() == null) {
                continue;
            }
            String questionVersionId = mapping.getQuestionVersionId();
            QuestionVersion questionVersion = questionVersions.get(questionVersionId);
            if (questionVersion == null) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Question version not found"));
            }
            validateAnswerJson(questionVersion, item.getAnswerJson());
        }
    }

    private Map<String, UserAnswer> loadExistingAnswers(String attemptId, List<String> examVersionQuestionIds) {
        List<UserAnswer> answers = userAnswerRepository
                .findByAttemptIdAndExamVersionQuestionIdInAndDeletedFalse(attemptId, examVersionQuestionIds);
        Map<String, UserAnswer> map = new HashMap<>();
        for (UserAnswer answer : answers) {
            map.put(answer.getExamVersionQuestionId(), answer);
        }
        return map;
    }

    private List<UserAnswer> buildUpserts(String attemptId,
                                          List<AttemptAnswerSaveItem> items,
                                          Map<String, ExamVersionQuestion> mappings) {
        List<String> validQuestionIds = new ArrayList<>(mappings.keySet());
        Map<String, UserAnswer> existing = loadExistingAnswers(attemptId, validQuestionIds);
        List<UserAnswer> toSave = new ArrayList<>();
        for (AttemptAnswerSaveItem item : items) {
            String examVersionQuestionId = item.getExamVersionQuestionId();
            if (!mappings.containsKey(examVersionQuestionId)) {
                continue;
            }
            UserAnswer current = existing.get(examVersionQuestionId);
            if (item.getAnswerJson() == null) {
                if (current != null && !current.isDeleted()) {
                    current.setDeleted(true);
                    toSave.add(current);
                }
                continue;
            }
            if (current == null) {
                UserAnswer created = UserAnswer.builder()
                        .attemptId(attemptId)
                        .examVersionQuestionId(examVersionQuestionId)
                        .answerJson(item.getAnswerJson())
                        .gradingStatus(UserAnswerGradingStatus.UNGRADED.name())
                        .build();
                toSave.add(created);
                continue;
            }
            current.setAnswerJson(item.getAnswerJson());
            current.setGradingStatus(UserAnswerGradingStatus.UNGRADED.name());
            current.setEarnedPoints(null);
            current.setGraderId(null);
            current.setGradedAt(null);
            current.setDeleted(false);
            toSave.add(current);
        }
        return toSave;
    }

    private void validateAnswerJson(QuestionVersion questionVersion, AnswerJson answerJson) {
        QuestionType questionType = resolveQuestionType(questionVersion.getType());
        if (answerJson.getType() != null && !answerJson.getType().equalsIgnoreCase(questionType.name())) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Answer type mismatch"));
        }
        AnswerJson.Payload payload = answerJson.getPayload();
        if (payload == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Answer payload is required"));
        }
        QuestionContent content = questionVersion.getQuestionContent();
        GradingRules gradingRules = questionVersion.getGradingRules();
        switch (questionType) {
            case SINGLE_CHOICE -> validateSingleChoice(payload, content, gradingRules);
            case MULTIPLE_CHOICE -> validateMultipleChoice(payload, content, gradingRules);
            case SHORT_TEXT -> validateShortText(payload, gradingRules);
            case MATCHING -> validateMatching(payload, content, gradingRules);
            case FILL_BLANKS -> validateFillBlanks(payload, content, gradingRules);
            case ESSAY -> validateEssay(payload, gradingRules);
            case FILE_UPLOAD -> validateFileUpload(payload, content, gradingRules);
            default -> throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Unsupported question type"));
        }
    }

    private QuestionType resolveQuestionType(String type) {
        try {
            return QuestionType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid question type: " + type));
        }
    }

    private void validateSingleChoice(AnswerJson.Payload payload, QuestionContent content, GradingRules gradingRules) {
        List<String> selected = payload.getSelectedOptionIds();
        if (selected == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("selectedOptionIds is required"));
        }
        if (selected.size() > 1) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Only one option is allowed"));
        }
        validateOptionIds(selected, content, gradingRules);
    }

    private void validateMultipleChoice(AnswerJson.Payload payload, QuestionContent content, GradingRules gradingRules) {
        List<String> selected = payload.getSelectedOptionIds();
        if (selected == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("selectedOptionIds is required"));
        }
        Set<String> unique = new HashSet<>(selected);
        if (unique.size() != selected.size()) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Duplicate option ids"));
        }
        validateOptionIds(selected, content, gradingRules);
    }

    private void validateOptionIds(List<String> selected, QuestionContent content, GradingRules gradingRules) {
        if (content == null || content.getOptions() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Options are not defined"));
        }
        Set<String> validOptionIds = new HashSet<>();
        for (QuestionContent.Option option : content.getOptions()) {
            validOptionIds.add(option.getId());
        }
        for (String optionId : selected) {
            if (!validOptionIds.contains(optionId)) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid option id: " + optionId));
            }
        }
        if (gradingRules == null || gradingRules.getChoice() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Grading rules are not defined"));
        }
    }

    private void validateShortText(AnswerJson.Payload payload, GradingRules gradingRules) {
        if (payload.getText() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Text answer is required"));
        }
        if (gradingRules == null || gradingRules.getShortText() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Grading rules are not defined"));
        }
    }

    private void validateEssay(AnswerJson.Payload payload, GradingRules gradingRules) {
        if (payload.getText() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Text answer is required"));
        }
        if (gradingRules == null || gradingRules.getManual() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Grading rules are not defined"));
        }
    }

    private void validateMatching(AnswerJson.Payload payload, QuestionContent content, GradingRules gradingRules) {
        if (payload.getPairs() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Pairs are required"));
        }
        if (content == null || content.getMatching() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Matching content is required"));
        }
        if (gradingRules == null || gradingRules.getMatching() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Grading rules are not defined"));
        }
        if (content.getMatching().getLeftItems() == null || content.getMatching().getRightItems() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Matching items are required"));
        }
        Set<String> leftIds = new HashSet<>();
        for (QuestionContent.Item item : content.getMatching().getLeftItems()) {
            leftIds.add(item.getId());
        }
        Set<String> rightIds = new HashSet<>();
        for (QuestionContent.Item item : content.getMatching().getRightItems()) {
            rightIds.add(item.getId());
        }
        Set<String> usedLeft = new HashSet<>();
        Set<String> usedRight = new HashSet<>();
        for (AnswerJson.Pair pair : payload.getPairs()) {
            if (pair.getLeftId() == null || pair.getRightId() == null) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Matching pair ids are required"));
            }
            if (!leftIds.contains(pair.getLeftId()) || !rightIds.contains(pair.getRightId())) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid matching ids"));
            }
            if (!usedLeft.add(pair.getLeftId()) || !usedRight.add(pair.getRightId())) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Duplicate matching ids"));
            }
        }
    }

    private void validateFillBlanks(AnswerJson.Payload payload, QuestionContent content, GradingRules gradingRules) {
        if (payload.getBlanks() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Blank answers are required"));
        }
        if (gradingRules == null || gradingRules.getFillBlanks() == null || gradingRules.getFillBlanks().getBlanks() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Grading rules are not defined"));
        }
        Set<String> validBlankIds = new HashSet<>();
        for (GradingRules.BlankRule rule : gradingRules.getFillBlanks().getBlanks()) {
            validBlankIds.add(rule.getBlankId());
        }
        Set<String> wordBankIds = new HashSet<>();
        String inputKind = gradingRules.getFillBlanks().getInputKind();
        if (content != null && content.getBlanks() != null && content.getBlanks().getWordBank() != null) {
            for (QuestionContent.WordBankItem item : content.getBlanks().getWordBank()) {
                wordBankIds.add(item.getId());
            }
        }
        for (AnswerJson.BlankAnswer blank : payload.getBlanks()) {
            if (blank.getBlankId() == null || !validBlankIds.contains(blank.getBlankId())) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid blank id"));
            }
            if ("word_bank".equalsIgnoreCase(inputKind) || "select".equalsIgnoreCase(inputKind)) {
                List<String> selected = blank.getSelectedOptionIds();
                if (selected == null) {
                    throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("selectedOptionIds is required"));
                }
                for (String optionId : selected) {
                    if (!wordBankIds.contains(optionId)) {
                        throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid word bank option id"));
                    }
                }
            } else {
                if (blank.getValue() == null) {
                    throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Blank value is required"));
                }
            }
        }
    }

    private void validateFileUpload(AnswerJson.Payload payload, QuestionContent content, GradingRules gradingRules) {
        if (payload.getFiles() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Files are required"));
        }
        if (gradingRules == null || gradingRules.getManual() == null) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Grading rules are not defined"));
        }
        Integer maxFiles = null;
        List<String> allowedTypes = null;
        if (content != null && content.getFileUpload() != null) {
            maxFiles = content.getFileUpload().getMaxFiles();
            allowedTypes = content.getFileUpload().getAllowedMimeTypes();
        }
        if (maxFiles != null && payload.getFiles().size() > maxFiles) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Exceeded max files"));
        }
        for (AnswerJson.FileAnswer file : payload.getFiles()) {
            if (file.getFileId() == null || file.getFileId().isBlank()) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("fileId is required"));
            }
            if (allowedTypes != null && file.getMime() != null && !allowedTypes.contains(file.getMime())) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid file mime"));
            }
        }
    }
}
