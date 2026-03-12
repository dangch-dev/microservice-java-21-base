package pl.co.assessment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import pl.co.assessment.dto.*;
import pl.co.assessment.entity.Exam;
import pl.co.assessment.entity.ExamAttempt;
import pl.co.assessment.entity.ExamSession;
import pl.co.assessment.entity.ExamSessionAssignment;
import pl.co.assessment.entity.ExamSessionTargetType;
import pl.co.assessment.entity.ExamVersion;
import pl.co.assessment.repository.ExamAttemptRepository;
import pl.co.assessment.repository.ExamRepository;
import pl.co.assessment.repository.ExamSessionAssignmentRepository;
import pl.co.assessment.repository.ExamSessionRepository;
import pl.co.assessment.repository.ExamVersionRepository;
import pl.co.assessment.service.IdentityLookupService;
import pl.co.assessment.service.ExamSessionService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.http.InternalApiClient;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamSessionAssignmentRepository examSessionAssignmentRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamVersionRepository examVersionRepository;
    private final InternalApiClient internalApiClient;
    private final ObjectMapper objectMapper;
    private final IdentityLookupService identityLookupService;

    @Value("${internal.service.auth-service}")
    private String authServiceId;

    private static final String INTERNAL_GUEST_PATH = "/internal/guest";

    @Override
    @Transactional
    public ExamSessionResponse createSession(String examId, ExamSessionCreateRequest request) {
        if (request == null || request.getTargetType() == null) {
            throw new ApiException(ErrorCode.E221, "targetType is required");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new ApiException(ErrorCode.E221, "title is required");
        }
        // Create Exam Session
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Exam not found")));
        if (!StringUtils.hasText(exam.getPublishedExamVersionId())) {
            throw new ApiException(ErrorCode.E221, "Exam has no published version");
        }

        validateTimeRange(request.getStartAt(), request.getEndAt());

        // Create exam session
        String accessCode = normalizeAccessCode(request.getAccessCode());
        ExamSession examSession = ExamSession.builder()
                .examId(exam.getId())
                .title(request.getTitle().trim())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .targetType(request.getTargetType())
                .accessCode(accessCode)
                .build();
        // Set common code if not GUEST
        String commonSessionCode = generateUniqueCode();
        if(request.getTargetType() != ExamSessionTargetType.GUEST) {
            examSession.setCode(commonSessionCode);
        }
        ExamSession examSessionSaved = examSessionRepository.save(examSession);

        // Create Assignment
        List<ExamSessionAssignment> assignments = new ArrayList<>();

        if (request.getTargetType() == ExamSessionTargetType.GUEST) {
            List<GuestInfoRequest> guestInfo = normalizeGuestInfo(request.getGuestInfo());
            if (guestInfo.isEmpty()) {
                throw new ApiException(ErrorCode.E221, "guestInfo is required for GUEST session");
            }
            Set<String> reservedCodes = new HashSet<>();
            for (GuestInfoRequest guest : guestInfo) {
                // create guest
                String userId = createGuestUser(guest);
                // each GUEST has unique sessionCode
                String code = generateUniqueCode(reservedCodes);
                //buildGuestAssignment
                assignments.add(ExamSessionAssignment.builder()
                        .sessionId(examSessionSaved.getId())
                        .userId(userId)
                        .code(code)
                        .accessCode(accessCode)
                        .build());

            }
        } else {
            List<String> userIds = normalizeUserIds(request.getUserIds());
            if (userIds.isEmpty()) {
                throw new ApiException(ErrorCode.E221, "userIds is required for USER/CLASS session");
            }
            for (String userId : userIds) {
                assignments.add(ExamSessionAssignment.builder()
                        .sessionId(examSessionSaved.getId())
                        .userId(userId)
                        .code(commonSessionCode)
                        .accessCode(accessCode)
                        .build());
            }
        }

        examSessionAssignmentRepository.saveAll(assignments);

        int assignmentCount = (int) examSessionAssignmentRepository.countBySessionIdAndDeletedFalse(examSessionSaved.getId());
        return toResponse(examSessionSaved, assignmentCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSessionListItemResponse> listSessions(String examId,
                                                          Instant startTime,
                                                          Instant endTime,
                                                          String searchValue,
                                                          Integer page,
                                                          Integer size) {
        int pageValue = page == null ? 0 : page;
        int sizeValue = size == null ? 20 : size;
        PageRequest pageRequest = PageRequest.of(Math.max(pageValue, 0), Math.max(sizeValue, 1));
        Page<ExamSession> sessionsPage = examSessionRepository.findManagementSessions(
                StringUtils.hasText(examId) ? examId.trim() : null,
                startTime,
                endTime,
                StringUtils.hasText(searchValue) ? searchValue.trim() : null,
                pageRequest
        );
        List<ExamSession> sessions = sessionsPage.getContent();
        List<ExamSessionListItemResponse> responses = new ArrayList<>();
        Map<String, ExamVersion> examVersionMap = resolveExamVersions(sessions);
        for (ExamSession session : sessions) {
            ExamVersion version = examVersionMap.get(session.getExamId());
            String examName = version == null ? null : version.getName();
            String examDescription = version == null ? null : version.getDescription();
            responses.add(toListItemResponse(session, examName, examDescription));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamSessionDetailResponse getSession(String sessionId) {
        ExamSession session = examSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Session not found"));
        Exam exam = examRepository.findByIdAndDeletedFalse(session.getExamId()).orElse(null);
        ExamVersion version = resolveExamVersion(exam);
        String examName = version == null ? null : version.getName();
        String examDescription = version == null ? null : version.getDescription();
        List<ExamSessionAssignment> assignments =
                examSessionAssignmentRepository.findBySessionIdAndDeletedFalse(session.getId());
        Map<String, ExamAttempt> attemptMap = resolveAttempts(assignments);
        List<String> userIds = assignments.stream()
                .map(ExamSessionAssignment::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        var lookupMap = identityLookupService.lookupByIds(userIds);
        List<ExamSessionAssignmentDetailResponse> detailResponses = new ArrayList<>();
        for (ExamSessionAssignment assignment : assignments) {
            UserLookupResponse user = lookupMap.get(assignment.getUserId());
            String attemptId = assignment.getAttemptId();
            ExamAttempt attempt = StringUtils.hasText(attemptId) ? attemptMap.get(attemptId) : null;
            detailResponses.add(toDetailResponse(assignment, attempt, user));
        }
        return toDetailResponse(session, examName, examDescription, detailResponses);
    }

    @Override
    @Transactional
    public ExamSessionResponse updateSession(String sessionId, ExamSessionUpdateRequest request) {
        ExamSession session = examSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Session not found"));

        validateTimeRange(request.getStartAt(), request.getEndAt());

        session.setTitle(request.getTitle().trim());
        if (request.getStartAt() != null || request.getEndAt() != null) {
            session.setStartAt(request.getStartAt());
            session.setEndAt(request.getEndAt());
        }

        // Check Access Code is updated or not
        boolean accessCodeChanged = false;
        String newAccessCode = normalizeAccessCode(request.getAccessCode());
        if (!Objects.equals(newAccessCode, session.getAccessCode())) {
            session.setAccessCode(newAccessCode);
            accessCodeChanged = true;
        }

        ExamSession saved = examSessionRepository.save(session);

        // if Access Code has updated
        if (accessCodeChanged) {
            List<ExamSessionAssignment> assignments =
                    examSessionAssignmentRepository.findBySessionIdAndDeletedFalse(saved.getId());
            if (!assignments.isEmpty()) {
                for (ExamSessionAssignment assignment : assignments) {
                    assignment.setAccessCode(newAccessCode);
                }
                examSessionAssignmentRepository.saveAll(assignments);
            }
        }

        int assignmentCount = (int) examSessionAssignmentRepository.countBySessionIdAndDeletedFalse(saved.getId());
        return toResponse(saved, assignmentCount);
    }

    @Override
    @Transactional
    public ExamSessionResponse rotateSessionCode(String sessionId) {
        ExamSession session = examSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Session not found"));

        List<ExamSessionAssignment> assignments =
                examSessionAssignmentRepository.findBySessionIdAndDeletedFalse(sessionId);

        if (session.getTargetType() == ExamSessionTargetType.GUEST) {
            Set<String> reservedCodes = new HashSet<>();
            for (ExamSessionAssignment assignment : assignments) {
                assignment.setCode(generateUniqueCode(reservedCodes));
            }
        } else {
            String newCode = generateUniqueCode();
            session.setCode(newCode);
            for (ExamSessionAssignment assignment : assignments) {
                assignment.setCode(newCode);
            }
        }

        examSessionRepository.save(session);
        if (!assignments.isEmpty()) {
            examSessionAssignmentRepository.saveAll(assignments);
        }

        int assignmentCount = (int) examSessionAssignmentRepository.countBySessionIdAndDeletedFalse(session.getId());
        return toResponse(session, assignmentCount);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        ExamSession session = examSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Session not found"));
        session.setDeleted(true);
        examSessionRepository.save(session);

        List<ExamSessionAssignment> assignments =
                examSessionAssignmentRepository.findBySessionIdAndDeletedFalse(sessionId);
        if (!assignments.isEmpty()) {
            for (ExamSessionAssignment assignment : assignments) {
                assignment.setDeleted(true);
            }
            examSessionAssignmentRepository.saveAll(assignments);
        }
    }

    @Override
    @Transactional
    public List<ExamSessionAssignmentResponse> addAssignments(String sessionId, ExamSessionAssignmentRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.E221, "request is required");
        }
        ExamSession session = examSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Session not found"));
        if (session.getTargetType() == ExamSessionTargetType.GUEST) {
            throw new ApiException(ErrorCode.E221, "Assignments are not allowed for GUEST session");
        }
        List<String> userIds = normalizeUserIds(request.getUserIds());
        if (userIds.isEmpty()) {
            throw new ApiException(ErrorCode.E221, "userIds is required");
        }

        List<ExamSessionAssignment> existing =
                examSessionAssignmentRepository.findBySessionIdAndDeletedFalse(sessionId);
        Set<String> existingUserIds = new HashSet<>();
        for (ExamSessionAssignment assignment : existing) {
            if (assignment.getUserId() != null) {
                existingUserIds.add(assignment.getUserId());
            }
        }

        List<ExamSessionAssignment> toSave = new ArrayList<>();
        for (String userId : userIds) {
            if (existingUserIds.contains(userId)) {
                throw new ApiException(ErrorCode.E220, "Duplicate userId: " + userId);
            }
            toSave.add(ExamSessionAssignment.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .code(session.getCode())
                    .accessCode(session.getAccessCode())
                    .build());
        }
        examSessionAssignmentRepository.saveAll(toSave);

        List<ExamSessionAssignmentResponse> responses = new ArrayList<>();
        for (ExamSessionAssignment assignment : toSave) {
            responses.add(new ExamSessionAssignmentResponse(
                    assignment.getId(),
                    assignment.getSessionId(),
                    assignment.getUserId(),
                    assignment.getCode(),
                    assignment.getAccessCode(),
                    assignment.getAttemptId()
            ));
        }
        return responses;
    }

    @Override
    @Transactional
    public void deleteAssignment(String sessionId, String assignmentId) {
        ExamSessionAssignment assignment = examSessionAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Assignment not found"));
        if (!sessionId.equals(assignment.getSessionId())) {
            throw new ApiException(ErrorCode.E221, "Assignment does not belong to session");
        }
        assignment.setDeleted(true);
        examSessionAssignmentRepository.save(assignment);
    }

    private ExamSessionResponse toResponse(ExamSession session, int assignmentCount) {
        return new ExamSessionResponse(
                session.getId(),
                session.getExamId(),
                session.getTitle(),
                session.getStartAt(),
                session.getEndAt(),
                session.getTargetType(),
                session.getCode(),
                session.getAccessCode(),
                assignmentCount
        );
    }

    private ExamSessionListItemResponse toListItemResponse(ExamSession session,
                                                           String examName,
                                                           String examDescription) {
        return new ExamSessionListItemResponse(
                session.getId(),
                session.getExamId(),
                examName,
                examDescription,
                session.getTitle(),
                session.getStartAt(),
                session.getEndAt(),
                session.getTargetType(),
                session.getCode(),
                session.getAccessCode()
        );
    }

    private ExamSessionAssignmentDetailResponse toDetailResponse(ExamSessionAssignment assignment,
                                                                 ExamAttempt attempt,
                                                                 UserLookupResponse user) {
        return new ExamSessionAssignmentDetailResponse(
                assignment.getId(),
                assignment.getSessionId(),
                assignment.getUserId(),
                assignment.getCode(),
                assignment.getAccessCode(),
                assignment.getAttemptId(),
                attempt == null ? null : attempt.getStatus(),
                attempt == null ? null : attempt.getGradingStatus(),
                attempt == null ? null : attempt.getScore(),
                attempt == null ? null : attempt.getMaxScore(),
                attempt == null ? null : attempt.getPercent(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getPhoneNumber()
        );
    }

    private ExamSessionDetailResponse toDetailResponse(ExamSession session,
                                                       String examName,
                                                       String examDescription,
                                                       List<ExamSessionAssignmentDetailResponse> assignments) {
        return new ExamSessionDetailResponse(
                session.getId(),
                session.getExamId(),
                examName,
                examDescription,
                session.getTitle(),
                session.getStartAt(),
                session.getEndAt(),
                session.getTargetType(),
                session.getCode(),
                session.getAccessCode(),
                assignments
        );
    }

    private ExamVersion resolveExamVersion(Exam exam) {
        if (exam == null) {
            return null;
        }
        String versionId = StringUtils.hasText(exam.getPublishedExamVersionId())
                ? exam.getPublishedExamVersionId()
                : exam.getDraftExamVersionId();
        if (!StringUtils.hasText(versionId)) {
            return null;
        }
        return examVersionRepository.findByIdAndDeletedFalse(versionId).orElse(null);
    }

    private Map<String, ExamVersion> resolveExamVersions(List<ExamSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return Map.of();
        }
        List<String> examIds = sessions.stream()
                .map(ExamSession::getExamId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (examIds.isEmpty()) {
            return Map.of();
        }
        List<Exam> exams = examRepository.findByIdInAndDeletedFalse(examIds);
        Map<String, Exam> examMap = new LinkedHashMap<>();
        for (Exam exam : exams) {
            examMap.put(exam.getId(), exam);
        }
        List<String> versionIds = new ArrayList<>();
        for (ExamSession session : sessions) {
            Exam exam = examMap.get(session.getExamId());
            if (exam == null) {
                continue;
            }
            String versionId = StringUtils.hasText(exam.getPublishedExamVersionId())
                    ? exam.getPublishedExamVersionId()
                    : exam.getDraftExamVersionId();
            if (StringUtils.hasText(versionId)) {
                versionIds.add(versionId);
            }
        }
        if (versionIds.isEmpty()) {
            return Map.of();
        }
        List<ExamVersion> versions = examVersionRepository.findByIdInAndDeletedFalse(versionIds);
        Map<String, ExamVersion> versionMap = new LinkedHashMap<>();
        for (ExamVersion version : versions) {
            versionMap.put(version.getId(), version);
        }
        Map<String, ExamVersion> result = new LinkedHashMap<>();
        for (ExamSession session : sessions) {
            Exam exam = examMap.get(session.getExamId());
            if (exam == null) {
                continue;
            }
            String versionId = StringUtils.hasText(exam.getPublishedExamVersionId())
                    ? exam.getPublishedExamVersionId()
                    : exam.getDraftExamVersionId();
            if (StringUtils.hasText(versionId)) {
                result.put(session.getExamId(), versionMap.get(versionId));
            }
        }
        return result;
    }

    private Map<String, ExamAttempt> resolveAttempts(List<ExamSessionAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Map.of();
        }
        List<String> attemptIds = assignments.stream()
                .map(ExamSessionAssignment::getAttemptId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (attemptIds.isEmpty()) {
            return Map.of();
        }
        List<ExamAttempt> attempts = examAttemptRepository.findByIdInAndDeletedFalse(attemptIds);
        Map<String, ExamAttempt> map = new LinkedHashMap<>();
        for (ExamAttempt attempt : attempts) {
            map.put(attempt.getId(), attempt);
        }
        return map;
    }


    private void validateTimeRange(Instant startAt, Instant endAt) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new ApiException(ErrorCode.E221, "startAt must be before endAt");
        }
    }

    private List<String> normalizeUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String userId : userIds) {
            if (!StringUtils.hasText(userId)) {
                throw new ApiException(ErrorCode.E221, "Invalid userId");
            }
            String trimmed = userId.trim();
            if (seen.add(trimmed)) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private String normalizeAccessCode(String accessCode) {
        if (!StringUtils.hasText(accessCode)) {
            return null;
        }
        return accessCode.trim();
    }

    private List<GuestInfoRequest> normalizeGuestInfo(List<GuestInfoRequest> guestInfo) {
        if (guestInfo == null || guestInfo.isEmpty()) {
            return List.of();
        }
        List<GuestInfoRequest> normalized = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        for (GuestInfoRequest info : guestInfo) {
            if (info == null) {
                continue;
            }
            if (!StringUtils.hasText(info.getFullName()) || !StringUtils.hasText(info.getEmail())) {
                throw new ApiException(ErrorCode.E221, "Invalid guestInfo");
            }
            GuestInfoRequest copy = new GuestInfoRequest();
            copy.setFullName(info.getFullName().trim());
            String email = info.getEmail().trim();
            String emailKey = email.toLowerCase();
            if (!seenEmails.add(emailKey)) {
                throw new ApiException(ErrorCode.E221, "Duplicate guest email: " + email);
            }
            copy.setEmail(email);
            if (StringUtils.hasText(info.getPhoneNumber())) {
                copy.setPhoneNumber(info.getPhoneNumber().trim());
            }
            normalized.add(copy);
        }
        return normalized;
    }

    private String createGuestUser(GuestInfoRequest guest) {
        InternalGuestRequest request = new InternalGuestRequest();
        request.setFullName(guest.getFullName());
        request.setEmail(guest.getEmail());
        request.setPhoneNumber(guest.getPhoneNumber());

        try {
            ResponseEntity<InternalGuestApiResponse> response = internalApiClient.send(
                    authServiceId,
                    INTERNAL_GUEST_PATH,
                    HttpMethod.POST,
                    MediaType.APPLICATION_JSON,
                    null,
                    null,
                    request,
                    InternalGuestApiResponse.class,
                    true
            );
            InternalGuestApiResponse body = response.getBody();
            if (body == null) {
                throw new ApiException(ErrorCode.E305, "Guest creation failed");
            }
            if (!body.isSuccess()) {
                if (ErrorCode.E255.code().equalsIgnoreCase(body.getErrorCode())) {
                    return null;
                }
                throw new ApiException(ErrorCode.E305, "Guest creation failed");
            }
            InternalGuestResponse data = body.getData();
            if (data == null || !StringUtils.hasText(data.getUserId())) {
                throw new ApiException(ErrorCode.E305, "Guest creation failed");
            }
            return data.getUserId();
        } catch (RestClientResponseException ex) {
            if (isEmailAlreadyInUse(ex.getResponseBodyAsString())) {
                return null;
            }
            throw ex;
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = generateCode();
            if (isCodeAvailable(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique code");
    }

    private String generateUniqueCode(Set<String> reservedCodes) {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = generateCode();
            if (!isCodeAvailable(code)) {
                continue;
            }
            if (reservedCodes == null || reservedCodes.add(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique code");
    }

    private boolean isCodeAvailable(String code) {
        return !examSessionRepository.existsByCodeAndDeletedFalse(code)
                && !examSessionAssignmentRepository.existsByCodeAndDeletedFalse(code);
    }

    private String generateCode() {
        char[] buffer = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(CODE_ALPHABET.length());
            buffer[i] = CODE_ALPHABET.charAt(index);
        }
        return new String(buffer);
    }

    private boolean isEmailAlreadyInUse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String errorCode = root.path("errorCode").asText();
            return ErrorCode.E255.code().equalsIgnoreCase(errorCode);
        } catch (Exception ex) {
            return false;
        }
    }
}
