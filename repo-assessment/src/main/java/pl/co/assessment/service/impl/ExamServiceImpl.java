package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.ExamCreateRequest;
import pl.co.assessment.dto.ExamCreateResponse;
import pl.co.assessment.dto.ExamDraftChangeRequest;
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
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.repository.ExamEditorQuestionRow;
import pl.co.assessment.repository.ExamListRow;
import pl.co.assessment.repository.ExamRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.repository.QuestionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.service.ExamService;
import pl.co.assessment.service.QuestionService;
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
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamVersionQuestionRepository examVersionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionService questionService;

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
        boolean hasChanges = request.getQuestionChanges() != null && !request.getQuestionChanges().isEmpty();
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

        ExamVersion draftExam = examVersionRepository.findByIdAndExamIdAndDeletedFalse(draftId, exam.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.E420,
                        ErrorCode.E420.message("Draft exam version does not exist")));
        if (!ExamVersionStatus.DRAFT.name().equalsIgnoreCase(draftExam.getStatus())) {
            throw new ApiException(ErrorCode.E420,
                    ErrorCode.E420.message("Draft exam version does not exist"));
        }

        // Load active question mappings for draft
        Map<String, ExamVersionQuestion> mapQuestionByQuestionId = new HashMap<>();
        Set<Integer> usedOrders = new HashSet<>();
        List<ExamVersionQuestion> activeMappings = //active mapping
                examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(draftId);
        for (ExamVersionQuestion item : activeMappings) {
            mapQuestionByQuestionId.put(item.getQuestionId(), item);
            usedOrders.add(item.getQuestionOrder());
        }

        // Classify changes by action type
        List<ExamDraftChangeRequest> deleteChanges = new ArrayList<>();
        List<ExamDraftChangeRequest> addChanges = new ArrayList<>();
        List<ExamDraftChangeRequest> editChanges = new ArrayList<>();
        List<ExamDraftChangeRequest> reorderOnlyChanges = new ArrayList<>();

        if (hasChanges) {
            Set<String> seenQuestionIds = new HashSet<>();
            for (ExamDraftChangeRequest questionChange : request.getQuestionChanges()) {
                String questionId = questionChange.getQuestionId();
                if (!seenQuestionIds.add(questionId)) {
                    throw new ApiException(ErrorCode.E220,
                            ErrorCode.E220.message("Duplicate questionId, questionId: " + questionId));
                }
                if (Boolean.TRUE.equals(questionChange.getDeleted())) {
                    deleteChanges.add(questionChange);
                    continue;
                }
                Integer order = questionChange.getQuestionOrder();
                if (order == null || order <= 0) {
                    throw new ApiException(ErrorCode.E221,
                            ErrorCode.E221.message("Invalid questionOrder, questionId: " + questionId));
                }
                boolean hasType = questionChange.getType() != null && !questionChange.getType().isBlank();
                boolean hasContent = questionChange.getQuestionContent() != null;
                boolean hasRules = questionChange.getGradingRules() != null;
                boolean hasPayload = hasType || hasContent || hasRules;
                if (mapQuestionByQuestionId.containsKey(questionId)) {
                    if (hasPayload) {
                        if (!(hasType && hasContent && hasRules)) {
                            throw new ApiException(ErrorCode.E221,
                                    ErrorCode.E221.message("type, questionContent, gradingRules are required, questionId: " + questionId));
                        }
                        editChanges.add(questionChange);
                    } else {
                        reorderOnlyChanges.add(questionChange);
                    }
                } else {
                    if (!(hasType && hasContent && hasRules)) {
                        throw new ApiException(ErrorCode.E221,
                                ErrorCode.E221.message("type, questionContent, gradingRules are required, questionId: " + questionId));
                    }
                    addChanges.add(questionChange);
                }
            }
        }

        // Apply changes to draft questions
        Map<String, ExamVersionQuestion> deleteUpdatedById = new HashMap<>();
        // Apply DELETE: soft-delete requested mappings
        if (!deleteChanges.isEmpty()) {
            Integer maxOrder = examVersionQuestionRepository.findMaxQuestionOrderByExamVersionId(draftId);
            int deleteOrderBase = maxOrder == null ? 0 : maxOrder;
            int deleteOrderOffset = 1;
            for (ExamDraftChangeRequest change : deleteChanges) {
                String questionId = change.getQuestionId();
                ExamVersionQuestion target = mapQuestionByQuestionId.get(questionId);
                if (target == null) {
                    throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid questionId, questionId: " + questionId));
                }
                int oldOrder = target.getQuestionOrder();
                target.setQuestionOrder(deleteOrderBase + deleteOrderOffset);
                deleteOrderOffset++;
                target.setDeleted(true);
                deleteUpdatedById.put(target.getId(), target);
                mapQuestionByQuestionId.remove(questionId);
                usedOrders.remove(oldOrder);
            }
        }
        if (!deleteUpdatedById.isEmpty()) {
            examVersionQuestionRepository.saveAll(deleteUpdatedById.values());
            examVersionQuestionRepository.flush();
        }

        if (hasChanges) {
            Map<String, Integer> finalOrders = new HashMap<>();
            Set<Integer> seenOrders = new HashSet<>();

            for (ExamVersionQuestion mapping : mapQuestionByQuestionId.values()) {
                finalOrders.put(mapping.getQuestionId(), mapping.getQuestionOrder());
            }

            if (!editChanges.isEmpty()) {
                for (ExamDraftChangeRequest change : editChanges) {
                    String questionId = change.getQuestionId();
                    ExamVersionQuestion target = mapQuestionByQuestionId.get(questionId);
                    if (target == null) {
                        throw new ApiException(ErrorCode.E221,
                                ErrorCode.E221.message("Invalid questionId, questionId: " + questionId));
                    }
                    finalOrders.put(questionId, change.getQuestionOrder());
                }
            }

            if (!reorderOnlyChanges.isEmpty()) {
                for (ExamDraftChangeRequest change : reorderOnlyChanges) {
                    String questionId = change.getQuestionId();
                    ExamVersionQuestion target = mapQuestionByQuestionId.get(questionId);
                    if (target == null) {
                        throw new ApiException(ErrorCode.E221,
                                ErrorCode.E221.message("Invalid questionId, questionId: " + questionId));
                    }
                    finalOrders.put(questionId, change.getQuestionOrder());
                }
            }

            if (!addChanges.isEmpty()) {
                for (ExamDraftChangeRequest change : addChanges) {
                    String questionId = change.getQuestionId();
                    if (mapQuestionByQuestionId.containsKey(questionId)) {
                        throw new ApiException(ErrorCode.E220,
                                ErrorCode.E220.message("Duplicate questionId, questionId: " + questionId));
                    }
                    finalOrders.put(questionId, change.getQuestionOrder());
                }
            }

            for (Map.Entry<String, Integer> entry : finalOrders.entrySet()) {
                Integer order = entry.getValue();
                if (!seenOrders.add(order)) {
                    throw new ApiException(ErrorCode.E220,
                            ErrorCode.E220.message("Duplicate questionOrder: " + order + ", questionId: " + entry.getKey()));
                }
            }

            int finalCount = finalOrders.size();
            for (int i = 1; i <= finalCount; i++) {
                if (!seenOrders.contains(i)) {
                    throw new ApiException(ErrorCode.E221,
                            ErrorCode.E221.message("questionOrder must be continuous from 1..N"));
                }
            }

            Map<String, Integer> editNewOrders = new HashMap<>();
            Set<String> reorderQuestionIds = new HashSet<>();
            for (ExamVersionQuestion mapping : mapQuestionByQuestionId.values()) {
                Integer desiredOrder = finalOrders.get(mapping.getQuestionId());
                if (desiredOrder != null && !desiredOrder.equals(mapping.getQuestionOrder())) {
                    editNewOrders.put(mapping.getQuestionId(), desiredOrder);
                    reorderQuestionIds.add(mapping.getQuestionId());
                }
            }

            if (!reorderQuestionIds.isEmpty()) {
                examVersionQuestionRepository.bumpQuestionOrder(draftId, reorderQuestionIds, 1000000);
                examVersionQuestionRepository.flush();
            }

            Map<String, ExamVersionQuestion> mapQuestionUpdatedById = new HashMap<>();
            if (!reorderQuestionIds.isEmpty()) {
                for (String questionId : reorderQuestionIds) {
                    ExamVersionQuestion target = mapQuestionByQuestionId.get(questionId);
                    if (target == null) {
                        throw new ApiException(ErrorCode.E221,
                                ErrorCode.E221.message("Invalid questionId, questionId: " + questionId));
                    }
                    Integer newOrder = editNewOrders.get(questionId);
                    if (newOrder != null) {
                        target.setQuestionOrder(newOrder);
                        mapQuestionUpdatedById.put(target.getId(), target);
                    }
                }
            }

            // Apply ADD: create Question + QuestionVersion v1 + mapping
            if (!addChanges.isEmpty()) {
                for (ExamDraftChangeRequest change : addChanges) {
                    String questionId = change.getQuestionId();
                    Integer order = finalOrders.get(questionId);
                    String type = questionService.normalizeQuestionType(change.getType(), questionId);
                    questionService.processQuestionPayload(change, type);

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

                    mapQuestionByQuestionId.put(question.getId(), savedMapping);
                }
            }

            // Apply EDIT: create new QuestionVersion and update mapping/order
            if (!editChanges.isEmpty()) {
                for (ExamDraftChangeRequest change : editChanges) {
                    String questionId = change.getQuestionId();
                    ExamVersionQuestion target = mapQuestionByQuestionId.get(questionId);
                    String type = questionService.normalizeQuestionType(change.getType(), questionId);
                    questionService.processQuestionPayload(change, type);
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

            if (!mapQuestionUpdatedById.isEmpty()) {
                examVersionQuestionRepository.saveAll(mapQuestionUpdatedById.values());
            }
        }

        // Apply metadata updates to draft
        if (hasMetadata) {
            var metadata = request.getMetadata();
            draftExam.setName(metadata.getName());
            draftExam.setShuffleQuestions(metadata.getShuffleQuestions());
            draftExam.setShuffleOptions(metadata.getShuffleOptions());
            draftExam.setDescription(metadata.getDescription());
            draftExam.setDurationMinutes(metadata.getDurationMinutes());
            examVersionRepository.save(draftExam);
        }
    }

    @Override
    @Transactional
    public void discardDraft(String examId) {
        Exam exam = examRepository.findByIdAndDeletedFalseForUpdate(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Exam not found")));

        String draftId = exam.getDraftExamVersionId();
        if (draftId == null || draftId.isBlank()) {
            return;
        }

        ExamVersion draft = examVersionRepository.findByIdAndExamIdAndDeletedFalse(draftId, exam.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.E420,
                        ErrorCode.E420.message("Draft exam version does not exist")));
        if (!ExamVersionStatus.DRAFT.name().equalsIgnoreCase(draft.getStatus())) {
            throw new ApiException(ErrorCode.E420,
                    ErrorCode.E420.message("Draft exam version does not exist"));
        }

        draft.setDeleted(true);
        examVersionRepository.save(draft);

        exam.setDraftExamVersionId(null);
        examRepository.save(exam);
    }


}
