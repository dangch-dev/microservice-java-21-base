package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import pl.co.assessment.dto.ExamCreateRequest;
import pl.co.assessment.dto.ExamCreateResponse;
import pl.co.assessment.dto.ExamDraftChangeRequest;
import pl.co.assessment.dto.ExamDraftGroupRequest;
import pl.co.assessment.dto.ExamDraftSaveRequest;
import pl.co.assessment.dto.ExamDraftMetadataRequest;
import pl.co.assessment.dto.ExamEditorMetadata;
import pl.co.assessment.dto.ExamEditorQuestion;
import pl.co.assessment.dto.ExamEditorResponse;
import pl.co.assessment.dto.ExamFormImportRequest;
import pl.co.assessment.dto.ExamFormImportResponse;
import pl.co.assessment.dto.ExamListItemResponse;
import pl.co.assessment.dto.ExamPageResponse;
import pl.co.assessment.dto.ExamStatusUpdateRequest;
import pl.co.assessment.entity.Exam;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.entity.ExamVersionStatus;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.Question;
import pl.co.assessment.entity.QuestionGroup;
import pl.co.assessment.entity.QuestionGroupItem;
import pl.co.assessment.entity.QuestionGroupVersion;
import pl.co.assessment.entity.QuestionType;
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.entity.json.GroupPromptContent;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.projection.ExamEditorQuestionRow;
import pl.co.assessment.projection.ExamListRow;
import pl.co.assessment.repository.ExamRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.repository.QuestionRepository;
import pl.co.assessment.repository.QuestionGroupItemRepository;
import pl.co.assessment.repository.QuestionGroupRepository;
import pl.co.assessment.repository.QuestionGroupVersionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.service.ExamService;
import pl.co.assessment.service.QuestionGroupService;
import pl.co.assessment.service.QuestionService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.file.FileMeta;
import pl.co.common.http.InternalApiClient;
import pl.co.common.event.EventPublisher;
import pl.co.common.util.UlidGenerator;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final String STORAGE_UPLOAD_PATH = "/file/upload";

    private final ExamRepository examRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamVersionQuestionRepository examVersionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final QuestionGroupVersionRepository questionGroupVersionRepository;
    private final QuestionGroupItemRepository questionGroupItemRepository;
    private final QuestionService questionService;
    private final QuestionGroupService questionGroupService;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final InternalApiClient internalApiClient;

    @Value("${kafka.topics.file}")
    private String fileTopic;

    @Value("${internal.service.storage-service}")
    private String storageServiceId;

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
    public ExamPageResponse list(String searchValue, String categoryId, Boolean enabled, Integer page, Integer size) {
        int pageValue = page == null ? 0 : page;
        int sizeValue = size == null ? 20 : size;
        String normalized = (searchValue == null || searchValue.isBlank()) ? null : searchValue;
        String categoryNormalized = (categoryId == null || categoryId.isBlank()) ? null : categoryId;
        Boolean enabledFilter = enabled;
        PageRequest pageRequest = PageRequest.of(Math.max(pageValue, 0), Math.max(sizeValue, 1));
        Page<ExamListRow> result = examRepository.findExamList(normalized, categoryNormalized, enabledFilter, pageRequest);
        List<ExamListItemResponse> items = result.getContent().stream()
                .map(row -> new ExamListItemResponse(
                        row.getExamId(),
                        row.getExamVersionId(),
                        row.getDraftExamVersionId(),
                        row.getCategoryName(),
                        row.getName(),
                        row.getDescription(),
                        row.getStatus(),
                        row.getDurationMinutes(),
                        Boolean.TRUE.equals(row.getShuffleQuestions()),
                        Boolean.TRUE.equals(row.getShuffleOptions()),
                        Boolean.TRUE.equals(row.getEnabled())))
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
    @Transactional(readOnly = true)
    public ExamPageResponse listPublic(String searchValue, Integer page, Integer size) {
        int pageValue = page == null ? 0 : page;
        int sizeValue = size == null ? 20 : size;
        String normalized = (searchValue == null || searchValue.isBlank()) ? null : searchValue;
        PageRequest pageRequest = PageRequest.of(Math.max(pageValue, 0), Math.max(sizeValue, 1));
        Page<ExamListRow> result = examRepository.findPublicExamList(normalized, pageRequest);
        List<ExamListItemResponse> items = result.getContent().stream()
                .map(row -> new ExamListItemResponse(
                        row.getExamId(),
                        row.getExamVersionId(),
                        row.getDraftExamVersionId(),
                        row.getCategoryName(),
                        row.getName(),
                        row.getDescription(),
                        row.getStatus(),
                        row.getDurationMinutes(),
                        Boolean.TRUE.equals(row.getShuffleQuestions()),
                        Boolean.TRUE.equals(row.getShuffleOptions()),
                        Boolean.TRUE.equals(row.getEnabled())))
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
    public ExamEditorResponse requestEdit(String examId, Boolean forceNewDraft) {
        Exam exam = examRepository.findByIdAndDeletedFalseForUpdate(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Exam not found"));

        boolean forceNew = Boolean.TRUE.equals(forceNewDraft);
        ExamVersion editorVersion = null;
        String draftId = exam.getDraftExamVersionId();

        if (!forceNew && StringUtils.hasText(draftId)) {
            editorVersion = examVersionRepository.findByIdAndExamIdAndDeletedFalse(draftId, exam.getId())
                    .orElse(null);
        }

        if (forceNew && StringUtils.hasText(draftId)) {
            ExamVersion existingDraft = examVersionRepository.findByIdAndExamIdAndDeletedFalse(draftId, exam.getId())
                    .orElse(null);
            if (existingDraft != null && ExamVersionStatus.DRAFT.name().equalsIgnoreCase(existingDraft.getStatus())) {
                if (examVersionRepository.existsByExamIdAndVersionAndDeletedTrue(exam.getId(), existingDraft.getVersion())) {
                    int nextVersion = examVersionRepository.findMaxVersionByExamId(exam.getId()) + 1;
                    existingDraft.setVersion(nextVersion);
                }
                existingDraft.setDeleted(true);
                examVersionRepository.save(existingDraft);
            }
            exam.setDraftExamVersionId(null);
            examRepository.save(exam);
            editorVersion = null;
        }

        if (editorVersion == null && StringUtils.hasText(draftId)) {
            exam.setDraftExamVersionId(null);
            examRepository.save(exam);
        }

        if (editorVersion == null) {
            editorVersion = createDraftVersion(exam);
        }

        // Build response
        // Build metadata from editorVersion
        ExamEditorMetadata metadata = new ExamEditorMetadata(
                editorVersion.getName(),
                editorVersion.getDescription(),
                editorVersion.getDurationMinutes(),
                editorVersion.isShuffleQuestions(),
                editorVersion.isShuffleOptions(),
                editorVersion.getStatus(),
                exam.isEnabled()
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

        List<ExamVersionQuestion> mappings =
                examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(editorVersion.getId());
        var groups = questionGroupService.buildGroups(mappings);

        // TODO: lock exam

        return new ExamEditorResponse(
                metadata,
                questions,
                groups
        );
    }

    private ExamVersion createDraftVersion(Exam exam) {
        String publishedId = exam.getPublishedExamVersionId();
        ExamVersion published = null;
        if (StringUtils.hasText(publishedId)) {
            published = examVersionRepository.findByIdAndExamIdAndDeletedFalse(publishedId, exam.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.E227, "Exam version not found"));
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            int nextVersion = examVersionRepository.findMaxVersionByExamId(exam.getId()) + 1;
            try {
                ExamVersion draft = ExamVersion.builder()
                        .examId(exam.getId())
                        .version(nextVersion)
                        .name(published == null ? "Untitled Exam" : published.getName())
                        .description(published == null ? null : published.getDescription())
                        .status(ExamVersionStatus.DRAFT.name())
                        .durationMinutes(published == null ? null : published.getDurationMinutes())
                        .shuffleQuestions(published != null && published.isShuffleQuestions())
                        .shuffleOptions(published != null && published.isShuffleOptions())
                        .build();
                ExamVersion editorVersion = examVersionRepository.saveAndFlush(draft);

                if (published != null) {
                    List<ExamVersionQuestion> publishedQuestions =
                            examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(published.getId());
                    if (!publishedQuestions.isEmpty()) {
                        String editorVersionId = editorVersion.getId();
                        List<ExamVersionQuestion> draftQuestions = publishedQuestions.stream()
                                .map(item -> ExamVersionQuestion.builder()
                                        .examVersionId(editorVersionId)
                                        .questionId(item.getQuestionId())
                                        .questionVersionId(item.getQuestionVersionId())
                                        .questionOrder(item.getQuestionOrder())
                                        .groupVersionId(item.getGroupVersionId())
                                        .build())
                                .toList();
                        examVersionQuestionRepository.saveAll(draftQuestions);
                    }
                }

                exam.setDraftExamVersionId(editorVersion.getId());
                examRepository.save(exam);
                return editorVersion;
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                if (attempt == 2) {
                    throw ex;
                }
            }
        }

        throw new IllegalStateException("Failed to create draft version after retries");
    }

    @Override
    @Transactional
    public void saveDraft(String examId, ExamDraftSaveRequest request) {
        // Check Change
        boolean hasMetadata = request.getMetadata() != null;
        boolean hasChanges = request.getQuestionChanges() != null && !request.getQuestionChanges().isEmpty();
        boolean hasGroups = request.getGroups() != null;
        if (!hasMetadata && !hasChanges && !hasGroups) {
            return;
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
                    publishQuestionFiles(change);

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
                    publishQuestionFiles(change);
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

        if (hasGroups) {
            applyGroupChanges(request.getGroups(), mapQuestionByQuestionId);
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
    public ExamFormImportResponse importFromForm(ExamFormImportRequest request) {
        if (request == null || request.getForm() == null) {
            throw new ApiException(ErrorCode.E221, "form is required");
        }
        JsonNode form = request.getForm();
        String title = readText(form.path("info"), "title");
        String description = readText(form.path("info"), "description");
        if (title == null || title.isBlank()) {
            throw new ApiException(ErrorCode.E221, "form.info.title is required");
        }

        Exam exam = Exam.builder().build();
        Exam savedExam = examRepository.save(exam);

        ExamVersion version = ExamVersion.builder()
                .examId(savedExam.getId())
                .version(1)
                .name(title)
                .description(description)
                .status(ExamVersionStatus.DRAFT.name())
                .durationMinutes(null)
                .shuffleQuestions(false)
                .shuffleOptions(false)
                .build();
        ExamVersion savedVersion = examVersionRepository.save(version);

        savedExam.setDraftExamVersionId(savedVersion.getId());
        examRepository.save(savedExam);

        List<ExamDraftChangeRequest> changes = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int order = 1;
        int skipped = 0;
        List<String> pendingImages = new ArrayList<>();
        Map<String, FileMeta> imageCache = new HashMap<>();

        JsonNode items = form.path("items");
        if (items.isArray()) {
            for (JsonNode item : items) {
                ImportResult result = importItem(item, order, changes, warnings, pendingImages, imageCache);
                order = result.nextOrder;
                skipped += result.skipped;
            }
        }

        ExamDraftSaveRequest draftRequest = new ExamDraftSaveRequest();
        ExamDraftMetadataRequest metadata = new ExamDraftMetadataRequest();
        metadata.setName(title);
        metadata.setDescription(description);
        metadata.setDurationMinutes(null);
        metadata.setShuffleQuestions(false);
        metadata.setShuffleOptions(false);
        draftRequest.setMetadata(metadata);
        draftRequest.setQuestionChanges(changes);
        draftRequest.setGroups(null);

        saveDraft(savedExam.getId(), draftRequest);

        return ExamFormImportResponse.builder()
                .examId(savedExam.getId())
                .draftExamVersionId(savedVersion.getId())
                .importedCount(changes.size())
                .skippedCount(skipped)
                .warnings(warnings)
                .build();
    }

    private ImportResult importItem(JsonNode item,
                                    int startOrder,
                                    List<ExamDraftChangeRequest> changes,
                                    List<String> warnings,
                                    List<String> pendingImages,
                                    Map<String, FileMeta> imageCache) {
        int order = startOrder;
        int skipped = 0;

        String itemImageUrl = readImageUrl(item.path("imageItem").path("image"));
        if (itemImageUrl != null) {
            pendingImages.add(itemImageUrl);
        }

        JsonNode questionItem = item.path("questionItem");
        JsonNode questionGroupItem = item.path("questionGroupItem");

        if (!questionItem.isMissingNode() && questionItem.has("question")) {
            ExamDraftChangeRequest change = buildQuestionFromItem(item,
                    questionItem.path("question"),
                    order,
                    warnings,
                    pendingImages,
                    imageCache);
            if (change != null) {
                changes.add(change);
                order++;
            } else {
                skipped++;
            }
            return new ImportResult(order, skipped);
        }

        if (!questionGroupItem.isMissingNode()) {
            JsonNode questions = questionGroupItem.path("questions");
            JsonNode grid = questionGroupItem.path("grid");
            if (questions.isArray() && grid.has("columns")) {
                for (JsonNode rowQuestion : questions) {
                    ExamDraftChangeRequest change = buildQuestionFromGroupRow(item,
                            rowQuestion,
                            grid.path("columns"),
                            order,
                            warnings,
                            pendingImages,
                            imageCache);
                    if (change != null) {
                        changes.add(change);
                        order++;
                    } else {
                        skipped++;
                    }
                }
                return new ImportResult(order, skipped);
            }
        }

        return new ImportResult(order, skipped);
    }

    private ExamDraftChangeRequest buildQuestionFromItem(JsonNode item,
                                                         JsonNode question,
                                                         int order,
                                                         List<String> warnings,
                                                         List<String> pendingImages,
                                                         Map<String, FileMeta> imageCache) {
        String title = readText(item, "title");
        String description = readText(item, "description");
        List<String> imageUrls = takeImageUrls(pendingImages,
                item.path("questionItem").path("image"),
                question.path("image"));
        String prompt = buildPromptText(title, description);
        List<FileMeta> promptFiles = resolveImageFiles(imageUrls, imageCache, warnings);

        JsonNode grading = question.path("grading");
        BigDecimal pointValue = readPointValue(grading);

        JsonNode choiceQuestion = question.path("choiceQuestion");
        if (!choiceQuestion.isMissingNode()) {
            String type = readText(choiceQuestion, "type");
            return buildChoiceQuestion(prompt,
                    promptFiles,
                    choiceQuestion.path("options"),
                    grading,
                    type,
                    order,
                    pointValue,
                    warnings,
                    imageCache);
        }

        JsonNode textQuestion = question.path("textQuestion");
        if (!textQuestion.isMissingNode()) {
            return buildTextQuestion(prompt, promptFiles, grading, order, pointValue, warnings);
        }

        warnings.add("Unsupported question type at order " + order);
        return null;
    }

    private ExamDraftChangeRequest buildQuestionFromGroupRow(JsonNode item,
                                                             JsonNode rowQuestion,
                                                             JsonNode columns,
                                                             int order,
                                                             List<String> warnings,
                                                             List<String> pendingImages,
                                                             Map<String, FileMeta> imageCache) {
        String groupTitle = readText(item, "title");
        String rowTitle = readText(rowQuestion.path("rowQuestion"), "title");
        List<String> imageUrls = takeImageUrls(pendingImages,
                item.path("questionGroupItem").path("image"),
                rowQuestion.path("image"));
        String prompt = buildPromptText(joinTitle(groupTitle, rowTitle), null);
        List<FileMeta> promptFiles = resolveImageFiles(imageUrls, imageCache, warnings);

        JsonNode grading = rowQuestion.path("grading");
        BigDecimal pointValue = readPointValue(grading);
        return buildChoiceQuestion(prompt,
                promptFiles,
                columns.path("options"),
                grading,
                "RADIO",
                order,
                pointValue,
                warnings,
                imageCache);
    }

    private ExamDraftChangeRequest buildChoiceQuestion(String prompt,
                                                       List<FileMeta> promptFiles,
                                                       JsonNode optionsNode,
                                                       JsonNode grading,
                                                       String rawType,
                                                       int order,
                                                       BigDecimal pointValue,
                                                       List<String> warnings,
                                                       Map<String, FileMeta> imageCache) {
        List<OptionPayload> optionsPayload = readOptionPayloads(optionsNode);
        if (optionsPayload.isEmpty()) {
            warnings.add("Choice question missing options at order " + order);
            return null;
        }
        Map<String, String> valueToId = new HashMap<>();
        List<QuestionContent.Option> options = new ArrayList<>();
        for (int i = 0; i < optionsPayload.size(); i++) {
            String id = optionIdByIndex(i);
            OptionPayload payload = optionsPayload.get(i);
            String value = payload.value;
            valueToId.put(value, id);
            List<FileMeta> optionFiles = resolveImageFiles(singletonImage(payload.imageUrl), imageCache, warnings);
            String optionContent = value;
            options.add(QuestionContent.Option.builder()
                    .id(id)
                    .content(optionContent)
                    .files(nullIfEmpty(optionFiles))
                    .build());
        }

        List<String> correctOptionIds = mapCorrectOptions(grading, valueToId);
        if (correctOptionIds.isEmpty()) {
            warnings.add("Choice question missing correct answers at order " + order);
            return null;
        }

        QuestionContent content = QuestionContent.builder()
                .prompt(buildPromptContent(prompt, promptFiles))
                .options(options)
                .build();

        GradingRules rules = GradingRules.builder()
                .maxPoints(pointValue == null ? BigDecimal.ONE : pointValue)
                .choice(GradingRules.Choice.builder().correctOptionIds(correctOptionIds).build())
                .build();

        String normalizedType = "CHECKBOX".equalsIgnoreCase(rawType)
                ? QuestionType.MULTIPLE_CHOICE.name()
                : QuestionType.SINGLE_CHOICE.name();

        return buildChange(normalizedType, content, rules, order);
    }

    private ExamDraftChangeRequest buildTextQuestion(String prompt,
                                                     List<FileMeta> promptFiles,
                                                     JsonNode grading,
                                                     int order,
                                                     BigDecimal pointValue,
                                                     List<String> warnings) {
        List<String> accepted = readCorrectAnswers(grading);
        QuestionContent content = QuestionContent.builder()
                .prompt(buildPromptContent(prompt, promptFiles))
                .build();
        if (!accepted.isEmpty()) {
            GradingRules rules = GradingRules.builder()
                    .maxPoints(pointValue == null ? BigDecimal.ONE : pointValue)
                    .shortText(GradingRules.ShortText.builder()
                            .accepted(accepted)
                            .matchMethod("exact")
                            .build())
                    .build();
            return buildChange(QuestionType.SHORT_TEXT.name(), content, rules, order);
        }
        GradingRules rules = GradingRules.builder()
                .maxPoints(pointValue == null ? BigDecimal.ONE : pointValue)
                .manual(GradingRules.Manual.builder().autoMode(Boolean.FALSE).build())
                .build();
        return buildChange(QuestionType.ESSAY.name(), content, rules, order);
    }

    private ExamDraftChangeRequest buildChange(String type,
                                               QuestionContent content,
                                               GradingRules rules,
                                               int order) {
        ExamDraftChangeRequest change = new ExamDraftChangeRequest();
        change.setQuestionId(UlidGenerator.nextUlid());
        change.setQuestionOrder(order);
        change.setType(type);
        change.setQuestionContent(content);
        change.setGradingRules(rules);
        return change;
    }

    private List<OptionPayload> readOptionPayloads(JsonNode optionsNode) {
        List<OptionPayload> values = new ArrayList<>();
        if (optionsNode != null && optionsNode.isArray()) {
            for (JsonNode option : optionsNode) {
                String value = readText(option, "value");
                if (value != null && !value.isBlank()) {
                    String imageUrl = readImageUrl(option.path("image"));
                    values.add(new OptionPayload(value, imageUrl));
                }
            }
        }
        return values;
    }

    private List<String> mapCorrectOptions(JsonNode grading, Map<String, String> valueToId) {
        List<String> correctIds = new ArrayList<>();
        for (String value : readCorrectAnswers(grading)) {
            String id = valueToId.get(value);
            if (id != null) {
                correctIds.add(id);
            }
        }
        return correctIds;
    }

    private List<String> readCorrectAnswers(JsonNode grading) {
        List<String> values = new ArrayList<>();
        if (grading == null || grading.isMissingNode()) {
            return values;
        }
        JsonNode answers = grading.path("correctAnswers").path("answers");
        if (answers.isArray()) {
            for (JsonNode answer : answers) {
                String value = readText(answer, "value");
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private BigDecimal readPointValue(JsonNode grading) {
        if (grading == null || grading.isMissingNode()) {
            return null;
        }
        JsonNode pointValue = grading.get("pointValue");
        if (pointValue == null || pointValue.isNull()) {
            return null;
        }
        return BigDecimal.valueOf(pointValue.asDouble());
    }

    private String readImageUrl(JsonNode imageNode) {
        if (imageNode == null || imageNode.isMissingNode()) {
            return null;
        }
        String contentUri = readText(imageNode, "contentUri");
        return (contentUri == null || contentUri.isBlank()) ? null : contentUri;
    }

    private List<String> takeImageUrls(List<String> pendingImages, JsonNode... imageNodes) {
        List<String> imageUrls = new ArrayList<>();
        if (pendingImages != null && !pendingImages.isEmpty()) {
            imageUrls.addAll(pendingImages);
            pendingImages.clear();
        }
        if (imageNodes != null) {
            for (JsonNode imageNode : imageNodes) {
                String imageUrl = readImageUrl(imageNode);
                if (imageUrl != null && !imageUrl.isBlank()) {
                    imageUrls.add(imageUrl);
                }
            }
        }
        return imageUrls;
    }

    private String buildPromptText(String title, String description) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append(title.trim());
        }
        if (description != null && !description.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(description.trim());
        }
        if (sb.length() == 0) {
            return "Untitled";
        }
        return sb.toString();
    }

    private QuestionContent.Prompt buildPromptContent(String content, List<FileMeta> files) {
        return QuestionContent.Prompt.builder()
                .content(content)
                .files(nullIfEmpty(files))
                .build();
    }

    private List<FileMeta> nullIfEmpty(List<FileMeta> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }
        return files;
    }

    private List<String> singletonImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return List.of();
        }
        return List.of(imageUrl);
    }

    private String joinTitle(String groupTitle, String rowTitle) {
        if (groupTitle == null || groupTitle.isBlank()) {
            return rowTitle;
        }
        if (rowTitle == null || rowTitle.isBlank()) {
            return groupTitle;
        }
        return groupTitle.trim() + " " + rowTitle.trim();
    }

    private String readText(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null ? null : text.trim();
    }

    private String optionIdByIndex(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private List<FileMeta> resolveImageFiles(List<String> imageUrls,
                                             Map<String, FileMeta> cache,
                                             List<String> warnings) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<FileMeta> files = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            if (imageUrl == null) {
                continue;
            }
            String normalizedUrl = imageUrl.trim();
            if (normalizedUrl.isBlank()) {
                continue;
            }
            FileMeta cached = cache.get(normalizedUrl);
            if (cached != null) {
                files.add(cached);
                continue;
            }
            try {
                ImagePayload payload = downloadImage(normalizedUrl);
                if (payload == null) {
                    warnings.add("Failed to download image: " + normalizedUrl);
                    continue;
                }
                FileMeta uploaded = uploadImageToStorage(payload);
                if (uploaded != null) {
                    cache.put(normalizedUrl, uploaded);
                    files.add(uploaded);
                } else {
                    warnings.add("Failed to upload image: " + normalizedUrl);
                }
            } catch (Exception ex) {
                warnings.add("Failed to import image: " + normalizedUrl + " (" + ex.getMessage() + ")");
            }
        }
        return files;
    }

    private ImagePayload downloadImage(String imageUrl) throws Exception {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .build();
        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw new ApiException(ErrorCode.E281, "Image download failed with status " + response.statusCode());
        }
        String contentType = response.headers()
                .firstValue(HttpHeaders.CONTENT_TYPE)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String filename = buildImageFilename(imageUrl, contentType);
        return new ImagePayload(response.body(), contentType, filename);
    }

    private String buildImageFilename(String imageUrl, String contentType) {
        String fallback = "form-image";
        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath();
            if (path != null) {
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (!name.isBlank()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }
        String ext = "";
        if (contentType != null && contentType.contains("/")) {
            ext = "." + contentType.substring(contentType.indexOf('/') + 1).replace("+", "_");
        }
        return fallback + ext;
    }

    private FileMeta uploadImageToStorage(ImagePayload payload) {
        if (payload == null || payload.bytes == null || payload.bytes.length == 0) {
            return null;
        }
        ByteArrayResource resource = new ByteArrayResource(payload.bytes) {
            @Override
            public String getFilename() {
                return payload.filename;
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(payload.contentType));
        HttpEntity<Resource> filePart = new HttpEntity<>(resource, partHeaders);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", filePart);

        ApiResponse<?> body = internalApiClient.send(
                storageServiceId,
                STORAGE_UPLOAD_PATH,
                HttpMethod.POST,
                MediaType.MULTIPART_FORM_DATA,
                null,
                null,
                form,
                ApiResponse.class,
                true).getBody();
        if (body == null || !body.success() || body.data() == null) {
            return null;
        }
        return objectMapper.convertValue(body.data(), FileMeta.class);
    }

    private static class ImagePayload {
        private final byte[] bytes;
        private final String contentType;
        private final String filename;

        private ImagePayload(byte[] bytes, String contentType, String filename) {
            this.bytes = bytes;
            this.contentType = contentType;
            this.filename = filename;
        }
    }

    private static class ImportResult {
        private final int nextOrder;
        private final int skipped;

        private ImportResult(int nextOrder, int skipped) {
            this.nextOrder = nextOrder;
            this.skipped = skipped;
        }
    }

    private static class OptionPayload {
        private final String value;
        private final String imageUrl;

        private OptionPayload(String value, String imageUrl) {
            this.value = value;
            this.imageUrl = imageUrl;
        }
    }

    private void publishQuestionFiles(ExamDraftChangeRequest change) {
        QuestionContent content = change.getQuestionContent();
        if (content == null) {
            return;
        }
        List<FileMeta> files = new ArrayList<>();
        addFiles(files, content.getPrompt() == null ? null : content.getPrompt().getFiles());
        addFiles(files, content.getExplanation() == null ? null : content.getExplanation().getFiles());
        if (content.getOptions() != null) {
            for (QuestionContent.Option option : content.getOptions()) {
                if (option == null) {
                    continue;
                }
                addFiles(files, option.getFiles());
            }
        }
        if (content.getMatching() != null) {
            if (content.getMatching().getLeftItems() != null) {
                for (QuestionContent.Item item : content.getMatching().getLeftItems()) {
                    if (item == null) {
                        continue;
                    }
                    addFiles(files, item.getFiles());
                }
            }
            if (content.getMatching().getRightItems() != null) {
                for (QuestionContent.Item item : content.getMatching().getRightItems()) {
                    if (item == null) {
                        continue;
                    }
                    addFiles(files, item.getFiles());
                }
            }
        }
        publishFiles(files);
    }

    private void publishGroupFiles(ExamDraftGroupRequest group) {
        if (group == null || group.getPromptContent() == null || group.getPromptContent().getPrompt() == null) {
            return;
        }
        List<FileMeta> files = group.getPromptContent().getPrompt().getFiles();
        publishFiles(files);
    }

    private void addFiles(List<FileMeta> target, List<FileMeta> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        target.addAll(files);
    }

    private void publishFiles(List<FileMeta> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        List<String> fileIds = files.stream()
                .map(FileMeta::fileId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (fileIds.isEmpty()) {
            return;
        }
        eventPublisher.publish(fileTopic, null, fileIds);
    }

    private void applyGroupChanges(List<ExamDraftGroupRequest> groups,
                                   Map<String, ExamVersionQuestion> mapQuestionByQuestionId) {
        if (groups == null) {
            return;
        }
        // Full replace: when groups is provided, only questions in groups will keep assignments.
        if (groups.isEmpty()) {
            List<ExamVersionQuestion> updates = new ArrayList<>();
            for (ExamVersionQuestion mapping : mapQuestionByQuestionId.values()) {
                if (mapping.getGroupVersionId() != null) {
                    mapping.setGroupVersionId(null);
                    updates.add(mapping);
                }
            }
            if (!updates.isEmpty()) {
                examVersionQuestionRepository.saveAll(updates);
            }
            return;
        }

        Set<String> usedQuestionIds = new HashSet<>();
        Set<String> usedGroupIds = new HashSet<>();
        Map<String, String> desiredGroupVersionByQuestionId = new HashMap<>();
        List<QuestionGroupItem> newGroupItems = new ArrayList<>();

        Set<String> requestGroupIds = groups.stream()
                .map(ExamDraftGroupRequest::getGroupId)
                .collect(Collectors.toSet());
        Map<String, QuestionGroup> existingGroups = questionGroupRepository.findAllById(requestGroupIds).stream()
                .collect(Collectors.toMap(QuestionGroup::getId, item -> item));
        List<QuestionGroup> groupUpserts = new ArrayList<>();
        for (String groupId : requestGroupIds) {
            QuestionGroup group = existingGroups.get(groupId);
            if (group == null) {
                QuestionGroup created = new QuestionGroup();
                created.setId(groupId);
                groupUpserts.add(created);
            } else if (group.isDeleted()) {
                group.setDeleted(false);
                groupUpserts.add(group);
            }
        }
        if (!groupUpserts.isEmpty()) {
            questionGroupRepository.saveAll(groupUpserts);
        }

        List<String> existingGroupVersionIds = mapQuestionByQuestionId.values().stream()
                .map(ExamVersionQuestion::getGroupVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, QuestionGroupVersion> groupVersionById = new HashMap<>();
        Map<String, List<QuestionGroupItem>> groupItemsByVersion = new HashMap<>();
        Map<String, String> groupIdToGroupVersionId = new HashMap<>();
        Set<String> inconsistentGroupIds = new HashSet<>();
        if (!existingGroupVersionIds.isEmpty()) {
            List<QuestionGroupVersion> versions =
                    questionGroupVersionRepository.findByIdInAndDeletedFalse(existingGroupVersionIds);
            List<String> matchingGroupVersionIds = new ArrayList<>();
            for (QuestionGroupVersion version : versions) {
                if (!requestGroupIds.contains(version.getQuestionGroupId())) {
                    continue;
                }
                groupVersionById.put(version.getId(), version);
                matchingGroupVersionIds.add(version.getId());
                String groupId = version.getQuestionGroupId();
                String existing = groupIdToGroupVersionId.putIfAbsent(groupId, version.getId());
                if (existing != null && !existing.equals(version.getId())) {
                    inconsistentGroupIds.add(groupId);
                }
            }
            if (!matchingGroupVersionIds.isEmpty()) {
                List<QuestionGroupItem> items =
                        questionGroupItemRepository.findByQuestionGroupVersionIdInAndDeletedFalseOrderByItemOrderAsc(matchingGroupVersionIds);
                groupItemsByVersion = items.stream()
                        .collect(Collectors.groupingBy(QuestionGroupItem::getQuestionGroupVersionId));
            }
        }

        for (ExamDraftGroupRequest group : groups) {
            String groupId = group.getGroupId();
            if (!usedGroupIds.add(groupId)) {
                throw new ApiException(ErrorCode.E220,
                        ErrorCode.E220.message("Duplicate groupId, groupId: " + groupId));
            }
            Integer previousOrder = null;
            List<String> desiredQuestionVersionIds = new ArrayList<>();
            for (String questionId : group.getQuestionIds()) {
                if (!usedQuestionIds.add(questionId)) {
                    throw new ApiException(ErrorCode.E220,
                            ErrorCode.E220.message("Question already grouped, questionId: " + questionId));
                }
                ExamVersionQuestion mapping = mapQuestionByQuestionId.get(questionId);
                if (mapping == null) {
                    throw new ApiException(ErrorCode.E221,
                            ErrorCode.E221.message("Invalid questionId, questionId: " + questionId));
                }
                Integer order = mapping.getQuestionOrder();
                if (order == null || order <= 0) {
                    throw new ApiException(ErrorCode.E221,
                            ErrorCode.E221.message("Invalid questionOrder, questionId: " + questionId));
                }
                if (previousOrder != null && order != previousOrder + 1) {
                    throw new ApiException(ErrorCode.E221,
                            ErrorCode.E221.message("Group questions must be consecutive, groupId: " + groupId));
                }
                previousOrder = order;

                desiredQuestionVersionIds.add(mapping.getQuestionVersionId());
            }

            String currentGroupVersionId = groupIdToGroupVersionId.get(groupId);
            boolean unchanged = false;
            if (currentGroupVersionId != null && !inconsistentGroupIds.contains(groupId)) {
                QuestionGroupVersion currentVersion = groupVersionById.get(currentGroupVersionId);
                List<QuestionGroupItem> currentItems = groupItemsByVersion.get(currentGroupVersionId);
                if (currentVersion != null && currentItems != null) {
                    List<String> currentQuestionVersionIds = currentItems.stream()
                            .map(QuestionGroupItem::getQuestionVersionId)
                            .toList();
                    boolean samePrompt = samePromptContent(currentVersion.getPromptContent(), group.getPromptContent());
                    if (samePrompt && currentQuestionVersionIds.equals(desiredQuestionVersionIds)) {
                        unchanged = true;
                    }
                }
            }

            String targetGroupVersionId;
            if (unchanged) {
                targetGroupVersionId = currentGroupVersionId;
            } else {
                QuestionGroupVersion groupVersion = QuestionGroupVersion.builder()
                        .questionGroupId(groupId)
                        .promptContent(group.getPromptContent())
                        .build();
                QuestionGroupVersion savedVersion = questionGroupVersionRepository.save(groupVersion);
                publishGroupFiles(group);
                targetGroupVersionId = savedVersion.getId();
                int newItemOrder = 1;
                for (String questionVersionId : desiredQuestionVersionIds) {
                    newGroupItems.add(QuestionGroupItem.builder()
                            .questionGroupVersionId(targetGroupVersionId)
                            .questionVersionId(questionVersionId)
                            .itemOrder(newItemOrder++)
                            .build());
                }
            }

            for (String questionId : group.getQuestionIds()) {
                desiredGroupVersionByQuestionId.put(questionId, targetGroupVersionId);
            }
        }

        List<ExamVersionQuestion> mappingUpdates = new ArrayList<>();
        for (ExamVersionQuestion mapping : mapQuestionByQuestionId.values()) {
            String target = desiredGroupVersionByQuestionId.get(mapping.getQuestionId());
            if (!Objects.equals(mapping.getGroupVersionId(), target)) {
                mapping.setGroupVersionId(target);
                mappingUpdates.add(mapping);
            }
        }

        if (!newGroupItems.isEmpty()) {
            questionGroupItemRepository.saveAll(newGroupItems);
        }
        if (!mappingUpdates.isEmpty()) {
            examVersionQuestionRepository.saveAll(mappingUpdates);
        }
    }

    private boolean samePromptContent(GroupPromptContent current, GroupPromptContent incoming) {
        if (current == incoming) {
            return true;
        }
        if (current == null || incoming == null) {
            return false;
        }
        return objectMapper.valueToTree(current).equals(objectMapper.valueToTree(incoming));
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

        if (examVersionRepository.existsByExamIdAndVersionAndDeletedTrue(exam.getId(), draft.getVersion())) {
            int nextVersion = examVersionRepository.findMaxVersionByExamId(exam.getId()) + 1;
            draft.setVersion(nextVersion);
        }
        draft.setDeleted(true);
        examVersionRepository.save(draft);

        exam.setDraftExamVersionId(null);
        examRepository.save(exam);
    }

    @Override
    @Transactional
    public void publishDraft(String examId) {
        Exam exam = examRepository.findByIdAndDeletedFalseForUpdate(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Exam not found")));

        String draftId = exam.getDraftExamVersionId();
        if (draftId == null || draftId.isBlank()) {
            throw new ApiException(ErrorCode.E432);
        }

        ExamVersion draft = examVersionRepository.findByIdAndExamIdAndDeletedFalse(draftId, exam.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.E433));
        if (!ExamVersionStatus.DRAFT.name().equalsIgnoreCase(draft.getStatus())) {
            throw new ApiException(ErrorCode.E434);
        }
        if (!examVersionQuestionRepository.existsByExamVersionIdAndDeletedFalse(draftId)) {
            throw new ApiException(ErrorCode.E435);
        }

        String oldPublishedId = exam.getPublishedExamVersionId();
        if (oldPublishedId != null && !oldPublishedId.isBlank() && !oldPublishedId.equals(draftId)) {
            examVersionRepository.findByIdAndExamIdAndDeletedFalse(oldPublishedId, exam.getId())
                    .ifPresent(oldPublished -> {
                        oldPublished.setStatus(ExamVersionStatus.ARCHIVED.name());
                        examVersionRepository.save(oldPublished);
                    });
        }

        draft.setStatus(ExamVersionStatus.PUBLISHED.name());
        examVersionRepository.save(draft);

        exam.setPublishedExamVersionId(draftId);
        exam.setDraftExamVersionId(null);
        examRepository.save(exam);
    }

    @Override
    @Transactional
    public void updateStatus(String examId, ExamStatusUpdateRequest request) {
        Exam exam = examRepository.findByIdAndDeletedFalseForUpdate(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Exam not found")));

        Boolean enabled = request == null ? null : request.getEnabled();
        if (enabled == null) {
            throw new ApiException(ErrorCode.E221);
        }
        if (exam.isEnabled() == enabled) {
            if (enabled) {
                throw new ApiException(ErrorCode.E204, ErrorCode.E204.message("Exam status is already enabled"));
            }
            throw new ApiException(ErrorCode.E204, ErrorCode.E204.message("Exam status is already disabled"));
        }
        if (enabled) {
            String publishedId = exam.getPublishedExamVersionId();
            if (publishedId == null || publishedId.isBlank()) {
                throw new ApiException(ErrorCode.E428);
            }
            ExamVersion published = examVersionRepository
                    .findByIdAndExamIdAndDeletedFalse(publishedId, exam.getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.E428));
            if (!ExamVersionStatus.PUBLISHED.name().equalsIgnoreCase(published.getStatus())) {
                throw new ApiException(ErrorCode.E431);
            }
        }

        exam.setEnabled(enabled);
        examRepository.save(exam);
    }

    @Override
    @Transactional
    public void deleteExam(String examId) {
        Exam exam = examRepository.findByIdAndDeletedFalseForUpdate(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Exam not found")));
        exam.setDeleted(true);
        examRepository.save(exam);
    }
}
