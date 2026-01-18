package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.ExamCreateRequest;
import pl.co.assessment.dto.ExamCreateResponse;
import pl.co.assessment.dto.ExamDraftChangeRequest;
import pl.co.assessment.dto.ExamDraftChangeType;
import pl.co.assessment.dto.ExamDraftMetadataRequest;
import pl.co.assessment.dto.ExamDraftSaveRequest;
import pl.co.assessment.dto.ExamEditorMetadata;
import pl.co.assessment.dto.ExamEditorQuestion;
import pl.co.assessment.dto.ExamEditorResponse;
import pl.co.assessment.dto.ExamListItemResponse;
import pl.co.assessment.dto.ExamPageResponse;
import pl.co.assessment.entity.Exam;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.entity.ExamVersionStatus;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.Question;
import pl.co.assessment.entity.QuestionType;
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.repository.ExamEditorQuestionRow;
import pl.co.assessment.repository.ExamListRow;
import pl.co.assessment.repository.ExamRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.repository.QuestionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.service.ExamService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private static final int SCHEMA_VERSION = 1;

    private final ExamRepository examRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamVersionQuestionRepository examVersionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;

    @Override
    @Transactional
    public ExamCreateResponse create(ExamCreateRequest request) {
        Exam exam = Exam.builder()
                .categoryId(request.getCategoryId())
                .build();
        Exam savedExam = examRepository.save(exam);

        ExamVersion version = ExamVersion.builder()
                .examId(savedExam.getId())
                .version(1)
                .name(request.getName())
                .description(request.getDescription())
                .status(ExamVersionStatus.DRAFT.name())
                .durationMinutes(request.getDurationMinutes())
                .shuffleQuestions(false)
                .shuffleOptions(false)
                .build();
        ExamVersion savedVersion = examVersionRepository.save(version);

        savedExam.setDraftExamVersionId(savedVersion.getId());
        examRepository.save(savedExam);

        return new ExamCreateResponse(savedExam.getId(), savedVersion.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamPageResponse list(String searchValue, String categoryId, Integer page, Integer size) {
        int pageValue = page == null ? 0 : page;
        int sizeValue = size == null ? 20 : size;
        String normalized = (searchValue == null || searchValue.isBlank()) ? null : searchValue;
        String categoryNormalized = (categoryId == null || categoryId.isBlank()) ? null : categoryId;
        PageRequest pageRequest = PageRequest.of(Math.max(pageValue, 0), Math.max(sizeValue, 1));
        Page<ExamListRow> result = examRepository.findExamList(normalized, categoryNormalized, pageRequest);
        List<ExamListItemResponse> items = result.getContent().stream()
                .map(row -> new ExamListItemResponse(
                        row.getExamId(),
                        row.getExamVersionId(),
                        row.getCategoryName(),
                        row.getName(),
                        row.getStatus(),
                        row.getDurationMinutes(),
                        Boolean.TRUE.equals(row.getShuffleQuestions()),
                        Boolean.TRUE.equals(row.getShuffleOptions())))
                .toList();
        return ExamPageResponse.builder()
                .items(items)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
    }

    @Override
    @Transactional
    public ExamEditorResponse requestEdit(String examId) {
        // Load exam (active only)
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Exam not found"));

        // Resolve editorVersion: prefer draft if it is valid for this exam
        ExamVersion editorVersion = null;
        String draftId = exam.getDraftExamVersionId();
        if (draftId != null && !draftId.isBlank()) {
            editorVersion = examVersionRepository.findByIdAndExamIdAndDeletedFalse(draftId, exam.getId())
                    .orElse(null);
        }
        // Clear stale draft pointer
        if (editorVersion == null && draftId != null && !draftId.isBlank()) {
            exam.setDraftExamVersionId(null);
            examRepository.save(exam);
        }

        // If no draft, create a new draft (empty or cloned from published)
        if (editorVersion == null) {
            String publishedId = exam.getPublishedExamVersionId();
            int nextVersion = examVersionRepository.findMaxVersionByExamIdAndDeletedFalse(exam.getId()) + 1;
            // If it doesn't have publishedId   => Create empty draft
            // Else                             => Create draft clone from publishedId version
            if (publishedId == null || publishedId.isBlank()) {
                ExamVersion draft = ExamVersion.builder()
                        .examId(exam.getId())
                        .version(nextVersion)
                        .name("Untitled Exam")
                        .description(null)
                        .status(ExamVersionStatus.DRAFT.name())
                        .durationMinutes(null)
                        .shuffleQuestions(false)
                        .shuffleOptions(false)
                        .build();
                editorVersion = examVersionRepository.save(draft);
            } else {
                ExamVersion published = examVersionRepository.findByIdAndExamIdAndDeletedFalse(publishedId, exam.getId())
                        .orElseThrow(() -> new ApiException(ErrorCode.E227, "Exam version not found"));

                ExamVersion draft = ExamVersion.builder()
                        .examId(exam.getId())
                        .version(nextVersion)
                        .name(published.getName())
                        .description(published.getDescription())
                        .status(ExamVersionStatus.DRAFT.name())
                        .durationMinutes(published.getDurationMinutes())
                        .shuffleQuestions(published.isShuffleQuestions())
                        .shuffleOptions(published.isShuffleOptions())
                        .build();
                editorVersion = examVersionRepository.save(draft);

                // Clone question order from published to draft
                List<ExamVersionQuestion> publishedQuestions = examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(published.getId());
                if (!publishedQuestions.isEmpty()) {
                    String editorVersionId = editorVersion.getId();
                    List<ExamVersionQuestion> draftQuestions = publishedQuestions.stream()
                            .map(item -> ExamVersionQuestion.builder()
                                    .examVersionId(editorVersionId)
                                    .questionId(item.getQuestionId())
                                    .questionVersionId(item.getQuestionVersionId())
                                    .questionOrder(item.getQuestionOrder())
                                    .build())
                            .toList();
                    examVersionQuestionRepository.saveAll(draftQuestions);
                }
            }
            // Persist draft pointer on exam
            exam.setDraftExamVersionId(editorVersion.getId());
            examRepository.save(exam);
        }

        // Build response
        // Build metadata from editorVersion
        String mode = editorVersion.getStatus() == null ? null : editorVersion.getStatus().toLowerCase();

        ExamEditorMetadata metadata = new ExamEditorMetadata(
                editorVersion.getName(),
                editorVersion.getDescription(),
                editorVersion.getDurationMinutes(),
                editorVersion.isShuffleQuestions(),
                editorVersion.isShuffleOptions()
        );

        // Load questions for editorVersion
        List<ExamEditorQuestionRow> rows = examRepository.findEditorQuestionsByVersionId(editorVersion.getId());
        List<ExamEditorQuestion> questions = new ArrayList<>();
        for (ExamEditorQuestionRow row : rows) {
            questions.add(new ExamEditorQuestion(
                    row.getQuestionId(),
                    row.getQuestionOrder(),
                    row.getQuestionVersionId(),
                    row.getType(),
                    row.getQuestionContent(),
                    row.getGradingRules()
            ));
        }

        return new ExamEditorResponse(
                mode,
                editorVersion.getId(),
                metadata,
                questions
        );
    }

    @Override
    @Transactional
    public void saveDraft(String examId, ExamDraftSaveRequest request) {
        // Check Change
        boolean hasMetadata = request.getMetadata() != null;
        boolean hasChanges = request.getChanges() != null && !request.getChanges().isEmpty();
        if (!hasMetadata && !hasChanges) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("metadata or changes is required"));
        }

        // Get exam
        Exam exam = examRepository.findByIdAndDeletedFalseForUpdate(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Exam not found")));

        // Get Draft
        String draftId = exam.getDraftExamVersionId();
        if (draftId == null || draftId.isBlank()) {
            throw new ApiException(ErrorCode.E420,
                    ErrorCode.E420.message("Draft exam version does not exist"));
        }

        ExamVersion draft = examVersionRepository.findByIdAndExamIdAndDeletedFalse(draftId, exam.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.E420,
                        ErrorCode.E420.message("Draft exam version does not exist")));
        if (!ExamVersionStatus.DRAFT.name().equalsIgnoreCase(draft.getStatus())) {
            throw new ApiException(ErrorCode.E420,
                    ErrorCode.E420.message("Draft exam version does not exist"));
        }

        // Load active question mappings for draft
        Map<String, ExamVersionQuestion> mapQuestionByQuestionId = new HashMap<>();
        Set<Integer> usedOrders = new HashSet<>();
        List<ExamVersionQuestion> activeMappings =
                examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(draftId);
        for (ExamVersionQuestion item : activeMappings) {
            mapQuestionByQuestionId.put(item.getQuestionId(), item);
            usedOrders.add(item.getQuestionOrder());
        }

        // Classify changes by action type
        List<ExamDraftChangeRequest> deleteChanges = new ArrayList<>();
        List<ExamDraftChangeRequest> addChanges = new ArrayList<>();
        List<ExamDraftChangeRequest> editChanges = new ArrayList<>();
        List<ExamDraftChangeRequest> reorderChanges = new ArrayList<>();

        if (hasChanges) {
            for (ExamDraftChangeRequest change : request.getChanges()) {
                EnumSet<ExamDraftChangeType> types = EnumSet.copyOf(change.getChangeTypes());
                if (types.contains(ExamDraftChangeType.DELETE)) {
                    deleteChanges.add(change);
                }
                if (types.contains(ExamDraftChangeType.ADD)) {
                    addChanges.add(change);
                }
                if (types.contains(ExamDraftChangeType.EDIT_CONTENT)) {
                    editChanges.add(change);
                }
                if (types.contains(ExamDraftChangeType.REORDER)) {
                    reorderChanges.add(change);
                }
            }
        }

        // Apply changes to draft questions
        Map<String, ExamVersionQuestion> mapQuestionUpdatedById = new HashMap<>();
        // Apply DELETE: soft-delete requested mappings
        if (!deleteChanges.isEmpty()) {
            for (ExamDraftChangeRequest change : deleteChanges) {
                String questionId = change.getQuestionId();
                ExamVersionQuestion target = mapQuestionByQuestionId.get(questionId);
                if (target == null) {
                    throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid questionId, questionId: " + questionId));
                }
                target.setDeleted(true);
                mapQuestionUpdatedById.put(target.getId(), target);
                mapQuestionByQuestionId.remove(questionId);
                usedOrders.remove(target.getQuestionOrder());
            }
        }

        // Apply ADD: create Question + QuestionVersion v1 + mapping
        if (!addChanges.isEmpty()) {
            for (ExamDraftChangeRequest change : addChanges) {
                String questionId = change.getQuestionId();
                Integer order = change.getQuestionOrder();
                if (usedOrders.contains(order)) {
                    throw new ApiException(ErrorCode.E220,
                            ErrorCode.E220.message("Duplicate questionOrder: " + order + ", questionId: " + questionId));
                }
                if (mapQuestionByQuestionId.containsKey(questionId)) {
                    throw new ApiException(ErrorCode.E220,
                            ErrorCode.E220.message("Duplicate questionId, questionId: " + questionId));
                }
                String type = normalizeQuestionType(change.getType(), questionId);
                validateQuestionPayloadByType(change, type);
                normalizeQuestionPayload(change, type);

                Question question = new Question();
                question.setId(questionId);
                questionRepository.save(question);
                QuestionVersion questionVersion = QuestionVersion.builder()
                        .questionId(question.getId())
                        .version(1)
                        .type(type)
                        .questionContent(change.getQuestionContent())
                        .gradingRules(change.getGradingRules())
                        .build();
                QuestionVersion savedQuestionVersion = questionVersionRepository.save(questionVersion);

                ExamVersionQuestion mapping = ExamVersionQuestion.builder()
                        .examVersionId(draftId)
                        .questionId(question.getId())
                        .questionVersionId(savedQuestionVersion.getId())
                        .questionOrder(order)
                        .build();
                ExamVersionQuestion savedMapping = examVersionQuestionRepository.save(mapping);

                usedOrders.add(order);
                mapQuestionByQuestionId.put(question.getId(), savedMapping);
            }
        }

        // Apply EDIT_CONTENT: create new QuestionVersion and update mapping
        if (!editChanges.isEmpty()) {
            for (ExamDraftChangeRequest change : editChanges) {
                String questionId = change.getQuestionId();
                ExamVersionQuestion target = mapQuestionByQuestionId.get(questionId);
                if (target == null) {
                    throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid questionId, questionId: " + questionId));
                }
                String type = normalizeQuestionType(change.getType(), questionId);
                validateQuestionPayloadByType(change, type);
                normalizeQuestionPayload(change, type);
                int nextVersion = questionVersionRepository.findMaxVersionByQuestionIdAndDeletedFalse(questionId) + 1;
                QuestionVersion questionVersion = QuestionVersion.builder()
                        .questionId(questionId)
                        .version(nextVersion)
                        .type(type)
                        .questionContent(change.getQuestionContent())
                        .gradingRules(change.getGradingRules())
                        .build();
                QuestionVersion savedQuestionVersion = questionVersionRepository.save(questionVersion);
                target.setQuestionVersionId(savedQuestionVersion.getId());
                mapQuestionUpdatedById.put(target.getId(), target);
            }
        }

        // Apply REORDER: bump orders, then set final order values
        if (!reorderChanges.isEmpty()) {
            Map<String, Integer> reorderMap = new HashMap<>();
            Set<Integer> availableOrders = new HashSet<>(usedOrders);
            for (ExamDraftChangeRequest change : reorderChanges) {
                String questionId = change.getQuestionId();
                ExamVersionQuestion target = mapQuestionByQuestionId.get(questionId);
                if (target == null) {
                    throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid questionId, questionId: " + questionId));
                }
                availableOrders.remove(target.getQuestionOrder());
            }
            for (ExamDraftChangeRequest change : reorderChanges) {
                String questionId = change.getQuestionId();
                Integer order = change.getQuestionOrder();
                if (reorderMap.containsKey(questionId)) {
                    throw new ApiException(ErrorCode.E220,
                            ErrorCode.E220.message("Duplicate questionId, questionId: " + questionId));
                }
                if (availableOrders.contains(order)) {
                    throw new ApiException(ErrorCode.E220,
                            ErrorCode.E220.message("Duplicate questionOrder: " + order + ", questionId: " + questionId));
                }
                reorderMap.put(questionId, order);
                availableOrders.add(order);
            }

            examVersionQuestionRepository.bumpQuestionOrder(draftId, reorderMap.keySet(), 1000000);
            for (Map.Entry<String, Integer> entry : reorderMap.entrySet()) {
                ExamVersionQuestion target = mapQuestionByQuestionId.get(entry.getKey());
                target.setQuestionOrder(entry.getValue());
                mapQuestionUpdatedById.put(target.getId(), target);
            }
        }

        if (!mapQuestionUpdatedById.isEmpty()) {
            examVersionQuestionRepository.saveAll(mapQuestionUpdatedById.values());
        }

        // Apply metadata updates to draft
        if (hasMetadata) {
            var metadata = request.getMetadata();
            draft.setName(metadata.getName());
            draft.setShuffleQuestions(metadata.getShuffleQuestions());
            draft.setShuffleOptions(metadata.getShuffleOptions());
            draft.setDescription(metadata.getDescription());
            draft.setDurationMinutes(metadata.getDurationMinutes());
            examVersionRepository.save(draft);
        }
    }

    private void normalizeQuestionPayload(ExamDraftChangeRequest change, String normalizedType) {
        QuestionContent content = change.getQuestionContent();
        if (content != null) {
            content.setSchemaVersion(SCHEMA_VERSION);
        }
        GradingRules rules = change.getGradingRules();
        if (rules == null) {
            return;
        }
        rules.setSchemaVersion(SCHEMA_VERSION);
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
            } else if (content.getBlanks().getWordBank() != null && !content.getBlanks().getWordBank().isEmpty()) {
                content.getBlanks().setWordBank(null);
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
                    if (blank.getAccepted() != null && !blank.getAccepted().isEmpty()) {
                        blank.setAccepted(null);
                    }
                    if (blank.getMatchMethod() != null && !blank.getMatchMethod().isBlank()) {
                        blank.setMatchMethod(null);
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
                    if (blank.getCorrectOptionIds() != null && !blank.getCorrectOptionIds().isEmpty()) {
                        blank.setCorrectOptionIds(null);
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

    private String normalizeQuestionType(String type, String questionId) {
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

}
