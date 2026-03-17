package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.AttemptAnswerResponse;
import pl.co.assessment.dto.AttemptDetailResponse;
import pl.co.assessment.dto.AttemptListItemResponse;
import pl.co.assessment.dto.AttemptLockResponse;
import pl.co.assessment.dto.AttemptManagementListItemResponse;
import pl.co.assessment.dto.AttemptManagementPageResponse;
import pl.co.assessment.dto.AttemptManualGradingSaveItem;
import pl.co.assessment.dto.AttemptManualGradingSaveRequest;
import pl.co.assessment.dto.AttemptPageResponse;
import pl.co.assessment.dto.AttemptQuestionResponse;
import pl.co.assessment.dto.AttemptResultItemResponse;
import pl.co.assessment.dto.AttemptResultResponse;
import pl.co.assessment.dto.UserLookupResponse;
import pl.co.assessment.entity.AttemptOptionOrder;
import pl.co.assessment.entity.AttemptQuestionOrder;
import pl.co.assessment.entity.ExamAttempt;
import pl.co.assessment.entity.ExamAttemptGradingStatus;
import pl.co.assessment.entity.ExamAttemptStatus;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.entity.ExamVersionQuestion;
import pl.co.assessment.entity.QuestionVersion;
import pl.co.assessment.entity.UserAnswer;
import pl.co.assessment.entity.UserAnswerGradingStatus;
import pl.co.assessment.entity.json.AnswerJson;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.repository.AttemptOptionOrderRepository;
import pl.co.assessment.repository.AttemptQuestionOrderRepository;
import pl.co.assessment.repository.ExamAttemptRepository;
import pl.co.assessment.repository.ExamVersionQuestionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.repository.QuestionVersionRepository;
import pl.co.assessment.repository.UserAnswerRepository;
import pl.co.assessment.service.AttemptService;
import pl.co.assessment.service.AttemptSubmissionService;
import pl.co.assessment.service.ManualGradingLockService;
import pl.co.assessment.service.QuestionGroupService;
import pl.co.assessment.service.IdentityLookupService;
import pl.co.assessment.projection.AttemptListRow;
import pl.co.assessment.projection.AttemptManagementListRow;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.security.RoleName;
import pl.co.common.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptServiceImpl implements AttemptService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamVersionQuestionRepository examVersionQuestionRepository;
    private final AttemptQuestionOrderRepository attemptQuestionOrderRepository;
    private final AttemptOptionOrderRepository attemptOptionOrderRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final AttemptSubmissionService attemptSubmissionService;
    private final ManualGradingLockService manualGradingLockService;
    private final QuestionGroupService questionGroupService;
    private final IdentityLookupService identityLookupService;

    @Override
    @Transactional
    public AttemptDetailResponse getAttempt(String attemptId, String userId) {
        ExamAttempt attempt = loadAttemptOrThrow(attemptId);
        // assert owner;
        if (!userId.equals(attempt.getCreatedBy())) {
            throw new ApiException(ErrorCode.E230, ErrorCode.E230.message("No authority"));
        }

        ExamVersion version = loadVersionOrThrow(attempt);
        List<ExamVersionQuestion> mappings = resolveQuestionOrder(attempt, version);
        Map<String, QuestionVersion> questionVersionMap = loadQuestionVersions(mappings);
        Map<String, List<String>> optionOrderMap = resolveOptionOrderMap(attempt, version);

        Long remainingSeconds = computeRemainingSeconds(attempt, version);
        boolean inProgress = ExamAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus());
        List<AttemptQuestionResponse> questions = inProgress
                ? buildQuestions(mappings, questionVersionMap, optionOrderMap)
                : null;
        List<AttemptAnswerResponse> answers = null;
        if (inProgress) {
            answers = userAnswerRepository.findByAttemptIdAndDeletedFalse(attempt.getId()).stream()
                    .map(answer -> AttemptAnswerResponse.builder()
                            .examVersionQuestionId(answer.getExamVersionQuestionId())
                            .answerJson(answer.getAnswerJson())
                            .build())
                    .toList();
        }
        var groups = questionGroupService.buildGroups(mappings);

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
                .groups(groups)
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
        // Access: owner always allowed. If no auth, only guest attempt is allowed.
        if (StringUtils.hasText(userId)) {
            if (!userId.equals(attempt.getCreatedBy()) && !isGuestUser(attempt.getCreatedBy())) {
                throw new ApiException(ErrorCode.E230, ErrorCode.E230.message("No authority"));
            }
        } else if (!isGuestUser(attempt.getCreatedBy())) {
            throw new ApiException(ErrorCode.E230, ErrorCode.E230.message("No authority"));
        }

        if (ExamAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt is in progress"));
        }
        AttemptResultResponse response = buildAttemptResult(attempt, null);
        enrichAttemptCreator(response, attempt.getCreatedBy(), false);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptManagementPageResponse listManagementAttempts(String status,
                                                                String gradingStatus,
                                                                String examId,
                                                                String userId,
                                                                Instant fromTime,
                                                                Instant toTime,
                                                                Integer page,
                                                                Integer size) {
        int pageValue = page == null ? 0 : page;
        int sizeValue = size == null ? 20 : size;
        PageRequest pageRequest = PageRequest.of(Math.max(pageValue, 0), Math.max(sizeValue, 1));
        Page<AttemptManagementListRow> result = examAttemptRepository.findManagementAttemptList(
                status,
                gradingStatus,
                examId,
                userId,
                fromTime,
                toTime,
                pageRequest
        );
        List<AttemptManagementListItemResponse> items = result.getContent().stream()
                .map(row -> AttemptManagementListItemResponse.builder()
                        .attemptId(row.getAttemptId())
                        .examId(row.getExamId())
                        .examVersionId(row.getExamVersionId())
                        .createdBy(row.getCreatedBy())
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
        Map<String, UserLookupResponse> users = identityLookupService.lookupByIds(
                items.stream()
                        .map(AttemptManagementListItemResponse::getCreatedBy)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        List<AttemptManagementListItemResponse> enriched = items.stream()
                .map(item -> {
        UserLookupResponse user = users.get(item.getCreatedBy());
                    return AttemptManagementListItemResponse.builder()
                            .attemptId(item.getAttemptId())
                            .examId(item.getExamId())
                            .examVersionId(item.getExamVersionId())
                            .createdBy(item.getCreatedBy())
                            .creatorFullName(user == null ? null : user.getFullName())
                            .creatorAvatarUrl(user == null ? null : user.getAvatarUrl())
                            .creatorEmail(user == null ? null : user.getEmail())
                            .creatorRoleName(resolvePrimaryRole(user))
                            .name(item.getName())
                            .description(item.getDescription())
                            .durationMinutes(item.getDurationMinutes())
                            .status(item.getStatus())
                            .gradingStatus(item.getGradingStatus())
                            .startTime(item.getStartTime())
                            .endTime(item.getEndTime())
                            .score(item.getScore())
                            .maxScore(item.getMaxScore())
                            .percent(item.getPercent())
                            .build();
                })
                .toList();
        return AttemptManagementPageResponse.builder()
                .items(enriched)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
    }

    @Override
    @Transactional
    public AttemptResultResponse getManagementAttemptResult(String attemptId) {
        ExamAttempt attempt = loadAttemptOrThrow(attemptId);
        if (ExamAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt is in progress"));
        }
        AttemptResultResponse response = buildAttemptResult(attempt, null);
        enrichAttemptCreator(response, attempt.getCreatedBy(), true);
        return response;
    }

    @Override
    @Transactional
    public AttemptResultResponse getManagementAttemptManualGrading(String attemptId, String adminId, String sessionId) {
        // Validate attempt and reject in-progress attempts.
        ExamAttempt attempt = loadAttemptOrThrow(attemptId);
        if (ExamAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt is not gradable"));
        }
        if (ExamAttemptGradingStatus.AUTO_GRADING.name().equalsIgnoreCase(attempt.getGradingStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt is auto grading"));
        }
        // Acquire or renew the manual grading lock.
        AttemptLockResponse enrichedLock;
        try {
            AttemptLockResponse lock = manualGradingLockService.acquire(attemptId, adminId, sessionId);
            enrichedLock = enrichLock(lock);
        } catch (ApiException ex) {
            throw enrichLockException(ex);
        }
        // Return grading payload with lock info (answered items only).
        AttemptResultResponse response = buildAttemptResult(attempt, enrichedLock);
        enrichAttemptCreator(response, attempt.getCreatedBy(), true);
        List<AttemptResultItemResponse> items = response.getItems() == null
                ? List.of()
                : response.getItems().stream()
                .filter(this::hasAnswer)
                .toList();
        response.setItems(items);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptLockResponse heartbeatManualGrading(String attemptId, String adminId, String sessionId) {
        // Renew lock or raise E425 if lost.
        try {
            AttemptLockResponse lock = manualGradingLockService.renew(attemptId, adminId, sessionId);
            return enrichLock(lock);
        } catch (ApiException ex) {
            throw enrichLockException(ex);
        }
    }

    @Override
    @Transactional
    public AttemptLockResponse saveManualGrading(String attemptId,
                                                 String adminId,
                                                 String sessionId,
                                                 AttemptManualGradingSaveRequest request) {
        ExamAttempt attempt = loadAttemptOrThrow(attemptId);
        if (ExamAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(ErrorCode.E420, ErrorCode.E420.message("Attempt is not gradable"));
        }
        AttemptLockResponse lock;
        try {
            lock = manualGradingLockService.validate(attemptId, adminId, sessionId);
        } catch (ApiException ex) {
            throw enrichLockException(ex);
        }

        List<AttemptManualGradingSaveItem> items = request == null ? null : request.getItems();
        if (items == null || items.isEmpty()) {
            throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("items is required"));
        }
        Set<String> questionIds = items.stream()
                .map(AttemptManualGradingSaveItem::getExamVersionQuestionId)
                .collect(Collectors.toSet());

        ExamVersion version = loadVersionOrThrow(attempt);
        List<ExamVersionQuestion> mappings =
                examVersionQuestionRepository.findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(version.getId());
        Map<String, ExamVersionQuestion> mappingById = new HashMap<>();
        for (ExamVersionQuestion mapping : mappings) {
            mappingById.put(mapping.getId(), mapping);
        }
        for (String questionId : questionIds) {
            if (!mappingById.containsKey(questionId)) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid examVersionQuestionId"));
            }
        }
        Map<String, QuestionVersion> questionVersionById = loadQuestionVersions(mappings);

        List<UserAnswer> allAnswers = userAnswerRepository.findByAttemptIdAndDeletedFalse(attemptId);
        Map<String, UserAnswer> answers = new HashMap<>();
        for (UserAnswer answer : allAnswers) {
            answers.put(answer.getExamVersionQuestionId(), answer);
        }
        for (String questionId : questionIds) {
            if (!answers.containsKey(questionId)) {
                throw new ApiException(ErrorCode.E221, ErrorCode.E221.message("Invalid examVersionQuestionId"));
            }
        }

        Instant now = Instant.now();
        List<UserAnswer> updatedAnswers = new ArrayList<>();
        Set<String> updatedAnswerIds = new HashSet<>();
        for (AttemptManualGradingSaveItem item : items) {
            UserAnswer answer = answers.get(item.getExamVersionQuestionId());
            if (answer == null) {
                continue;
            }
            ExamVersionQuestion mapping = mappingById.get(item.getExamVersionQuestionId());
            if (mapping == null) {
                throw new ApiException(ErrorCode.E221,
                        ErrorCode.E221.message("Invalid examVersionQuestionId"));
            }
            QuestionVersion questionVersion = questionVersionById.get(mapping.getQuestionVersionId());
            BigDecimal maxPoints = null;
            if (questionVersion != null && questionVersion.getGradingRules() != null) {
                maxPoints = questionVersion.getGradingRules().getMaxPoints();
            }
            if (maxPoints != null
                    && item.getEarnedPoints() != null
                    && item.getEarnedPoints().compareTo(maxPoints) > 0) {
                throw new ApiException(ErrorCode.E221,
                        ErrorCode.E221.message("earnedPoints exceeds maxPoints for " + item.getExamVersionQuestionId()));
            }
            answer.setEarnedPoints(item.getEarnedPoints());
            answer.setGraderComment(item.getGraderComment());
            answer.setGraderId(adminId);
            answer.setGradedAt(now);
            answer.setGradingStatus(UserAnswerGradingStatus.FINALIZED.name());
            if (answer.getId() != null && updatedAnswerIds.add(answer.getId())) {
                updatedAnswers.add(answer);
            }
        }
        if (!updatedAnswers.isEmpty()) {
            userAnswerRepository.saveAll(updatedAnswers);
        }

        BigDecimal totalMax = BigDecimal.ZERO;
        BigDecimal totalScore = BigDecimal.ZERO;
        boolean manualPending = false;
        for (ExamVersionQuestion mapping : mappings) {
            QuestionVersion questionVersion = questionVersionById.get(mapping.getQuestionVersionId());
            if (questionVersion == null) {
                continue;
            }
            GradingRules rules = questionVersion.getGradingRules();
            BigDecimal maxPoints = rules == null || rules.getMaxPoints() == null
                    ? BigDecimal.ZERO
                    : rules.getMaxPoints();
            totalMax = totalMax.add(maxPoints);

            UserAnswer answer = answers.get(mapping.getId());
            if (answer == null) {
                continue;
            }
            if (answer.getEarnedPoints() != null) {
                totalScore = totalScore.add(answer.getEarnedPoints());
            }
            if (UserAnswerGradingStatus.MANUAL_PENDING.name().equalsIgnoreCase(answer.getGradingStatus())) {
                manualPending = true;
            }
        }

        attempt.setScore(totalScore);
        attempt.setMaxScore(totalMax);
        if (totalMax.compareTo(BigDecimal.ZERO) > 0) {
            attempt.setPercent(totalScore
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalMax, 2, RoundingMode.HALF_UP));
        } else {
            attempt.setPercent(null);
        }
        attempt.setGradingStatus(manualPending
                ? ExamAttemptGradingStatus.MANUAL_GRADING.name()
                : ExamAttemptGradingStatus.GRADED.name());

        return enrichLock(lock);
    }

    private AttemptLockResponse enrichLock(AttemptLockResponse lock) {
        if (lock == null || lock.getOwnerId() == null || lock.getOwnerId().isBlank()) {
            return lock;
        }
        Map<String, UserLookupResponse> users = identityLookupService.lookupByIds(Set.of(lock.getOwnerId()));
        UserLookupResponse user = users.get(lock.getOwnerId());
        if (user == null) {
            return lock;
        }
        return AttemptLockResponse.builder()
                .ownerId(lock.getOwnerId())
                .ownerFullName(user.getFullName())
                .ownerAvatarUrl(user.getAvatarUrl())
                .ownerEmail(user.getEmail())
                .ownerRoleName(resolvePrimaryRole(user))
                .sessionId(lock.getSessionId())
                .ttlSeconds(lock.getTtlSeconds())
                .build();
    }

    private void enrichAttemptCreator(AttemptResultResponse response, String userId, boolean includeRoleName) {
        if (response == null || !StringUtils.hasText(userId)) {
            return;
        }
        Map<String, UserLookupResponse> users = identityLookupService.lookupByIds(Set.of(userId));
        UserLookupResponse user = users.get(userId);
        response.setCreatedBy(userId);
        response.setCreatorFullName(user == null ? null : user.getFullName());
        response.setCreatorAvatarUrl(user == null ? null : user.getAvatarUrl());
        response.setCreatorEmail(user == null ? null : user.getEmail());
        if (includeRoleName) {
            response.setCreatorRoleName(resolvePrimaryRole(user));
        }
    }

    private ApiException enrichLockException(ApiException ex) {
        if (ex == null) {
            return null;
        }
        Object data = ex.getData();
        if (data instanceof AttemptLockResponse lock) {
            AttemptLockResponse enriched = enrichLock(lock);
            return new ApiException(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), enriched, ex);
        }
        return ex;
    }

    private ExamAttempt loadAttemptOrThrow(String attemptId) {
        return examAttemptRepository.findByIdAndDeletedFalse(attemptId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Attempt not found")));
    }

    private String resolvePrimaryRole(UserLookupResponse user) {
        if (user == null || user.getRoleNames() == null || user.getRoleNames().isEmpty()) {
            return null;
        }
        return user.getRoleNames().get(0);
    }

    private boolean isGuestUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        Map<String, UserLookupResponse> users = identityLookupService.lookupByIds(Set.of(userId));
        UserLookupResponse user = users.get(userId);
        return user != null
                && user.getRoleNames() != null
                && user.getRoleNames().size() == 1
                && RoleName.ROLE_GUEST.name().equals(user.getRoleNames().get(0));
    }

    private ExamVersion loadVersionOrThrow(ExamAttempt attempt) {
        return examVersionRepository.findByIdAndDeletedFalse(attempt.getExamVersionId())
                .orElseThrow(() -> new ApiException(ErrorCode.E420, ErrorCode.E420.message("Exam version does not exist")));
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
                    .questionId(mapping.getQuestionId())
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
                    .questionId(mapping.getQuestionId())
                    .questionVersionId(qv.getId())
                    .type(qv.getType())
                    .questionContent(resolvedContent)
                    .gradingRules(qv.getGradingRules())
                    .answerJson(answer == null ? null : answer.getAnswerJson())
                    .earnedPoints(answer == null ? null : answer.getEarnedPoints())
                    .answerGradingStatus(answer == null ? null : answer.getGradingStatus())
                    .graderComment(answer == null ? null : answer.getGraderComment())
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

    private AttemptResultResponse buildAttemptResult(ExamAttempt attempt, AttemptLockResponse lock) {
        ExamVersion version = loadVersionOrThrow(attempt);
        List<ExamVersionQuestion> mappings = resolveQuestionOrder(attempt, version);
        Map<String, QuestionVersion> questionVersionMap = loadQuestionVersions(mappings);
        Map<String, List<String>> optionOrderMap = resolveOptionOrderMap(attempt, version);
        List<UserAnswer> answerList = userAnswerRepository.findByAttemptIdAndDeletedFalse(attempt.getId());
        Map<String, UserAnswer> answerMap = new HashMap<>();
        for (UserAnswer answer : answerList) {
            answerMap.put(answer.getExamVersionQuestionId(), answer);
        }
        List<AttemptResultItemResponse> items = buildResultItems(mappings, questionVersionMap, optionOrderMap, answerMap);
        var groups = questionGroupService.buildGroups(mappings);
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
                .groups(groups)
                .lock(lock)
                .build();
    }

    private boolean hasAnswer(AttemptResultItemResponse item) {
        AnswerJson answerJson = item.getAnswerJson();
        if (answerJson == null || answerJson.getPayload() == null) {
            return false;
        }
        AnswerJson.Payload payload = answerJson.getPayload();
        boolean hasSelected = payload.getSelectedOptionIds() != null && !payload.getSelectedOptionIds().isEmpty();
        boolean hasText = payload.getText() != null && !payload.getText().isBlank();
        boolean hasPairs = payload.getPairs() != null && !payload.getPairs().isEmpty();
        boolean hasBlanks = payload.getBlanks() != null && !payload.getBlanks().isEmpty();
        boolean hasFiles = payload.getFiles() != null && !payload.getFiles().isEmpty();
        return hasSelected || hasText || hasPairs || hasBlanks || hasFiles;
    }
}
