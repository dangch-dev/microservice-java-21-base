package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.assessment.dto.AttemptStartResponse;
import pl.co.assessment.service.AttemptStartService;
import pl.co.common.util.StringUtils;
import pl.co.assessment.dto.GetSessionResponse;
import pl.co.assessment.entity.*;
import pl.co.assessment.repository.*;
import pl.co.assessment.service.ExamSessionService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {
    private final ExamSessionRepository examSessionRepository;
    private final ExamSessionAssignmentRepository examSessionAssignmentRepository;
    private final ExamRepository examRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ExamAttemptRepository examAttemptRepository;

    private final AttemptStartService attemptStartService;

    @Override
    @Transactional
    public GetSessionResponse getSession(String code, String userId) {
        if (!StringUtils.hasText(code)) {
            throw new ApiException(ErrorCode.E221, "code is required");
        }
        ExamSessionAssignment assignment = examSessionAssignmentRepository.findByCodeAndUserIdAndDeletedFalse(code, userId).orElseThrow(
                () -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("code: " + code))
        );

        ExamSession session = examSessionRepository.findByIdAndDeletedFalse(assignment.getSessionId())
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Session not found"));

        // Check TIME
        Instant now = Instant.now();
        if (session.getStartAt() != null && now.isBefore(session.getStartAt())) {
            throw new ApiException(ErrorCode.E221, "Session has not started");
        }
        if (session.getEndAt() != null && now.isAfter(session.getEndAt())) {
            throw new ApiException(ErrorCode.E221, "Session has ended");
        }

        // Load exam
        Exam exam = loadExamOrThrow(session.getExamId());
        ExamVersion published = loadPublishedVersionOrThrow(exam);

        // If has active attempt
        ExamAttempt activeAttempt = findActiveAttempt(exam.getId(), userId);
        String activeAttemptId = activeAttempt == null ? null : activeAttempt.getId();
        // Case active attempt is current session's attempt
        if (assignment.getAttemptId() != null && assignment.getAttemptId().equals(activeAttemptId)) {
            activeAttemptId = null;
        }
        String sessionAttemptStatus = null;
        if (assignment.getAttemptId() != null) {
            ExamAttempt sessionAttempt = examAttemptRepository.findByIdAndDeletedFalse(assignment.getAttemptId()).orElse(null);
            if (sessionAttempt != null) {
                sessionAttemptStatus = sessionAttempt.getStatus();
            }
        }

        // Build response
        return GetSessionResponse.builder()
                .examId(exam.getId())
                .examVersionId(published.getId())
                .name(published.getName())
                .description(published.getDescription())
                .durationMinutes(published.getDurationMinutes())
                .startTime(session.getStartAt())
                .endTime(session.getEndAt())
                .requiredAccessCode(StringUtils.hasText(assignment.getAccessCode()))
                .sessionAttemptId(assignment.getAttemptId())
                .sessionAttemptStatus(sessionAttemptStatus)
                .activeAttemptId(activeAttemptId)
                .build();
    }

    @Override
    @Transactional
    public AttemptStartResponse startSession(String code, String userId, String accessCode) {
        ExamSessionAssignment assignment = examSessionAssignmentRepository.findByCodeAndUserIdAndDeletedFalse(code, userId).orElseThrow(
                () -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("code: " + code))
        );

        ExamSession session = examSessionRepository.findByIdAndDeletedFalse(assignment.getSessionId())
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Session not found"));

        // Check TIME
        Instant now = Instant.now();
        if (session.getStartAt() != null && now.isBefore(session.getStartAt())) {
            throw new ApiException(ErrorCode.E221, "Session has not started");
        }
        if (session.getEndAt() != null && now.isAfter(session.getEndAt())) {
            throw new ApiException(ErrorCode.E221, "Session has ended");
        }

        // Check active attempt of the same exam (ignore current session's attempt)
        ExamAttempt activeAttempt = findActiveAttempt(session.getExamId(), userId);
        if (activeAttempt != null) {
            String activeAttemptId = activeAttempt.getId();
            if (assignment.getAttemptId() == null || !assignment.getAttemptId().equals(activeAttemptId)) {
                throw new ApiException(ErrorCode.E420, "Submit active attempt before start a new attempt");
            }
        }

        // Validate access code
        validateAccessCode(assignment, accessCode);

        // If session already has an attempt, only allow resume
        if (assignment.getAttemptId() != null) {
            ExamAttempt sessionAttempt = examAttemptRepository.findByIdAndDeletedFalse(assignment.getAttemptId())
                    .orElseThrow(() -> new ApiException(ErrorCode.E227, "Attempt not found"));
            if (ExamAttemptStatus.SUBMITTED.name().equalsIgnoreCase(sessionAttempt.getStatus())) {
                throw new ApiException(ErrorCode.E420, "This session code has already been used");
            }
            return attemptStartService.startAttempt(session.getExamId(), userId, false);
        }

        // Create Attempt
        AttemptStartResponse attemptStart = attemptStartService.startAttempt(session.getExamId(),userId,false);

        // Save attempt to session
        assignment.setAttemptId(attemptStart.getAttemptId());
        examSessionAssignmentRepository.save(assignment);

        // Response
        return attemptStart;
    }

    private ExamAttempt findActiveAttempt(String examId, String userId) {
        // Lock active attempts to avoid double-start race
        List<String> statuses = List.of(
                ExamAttemptStatus.IN_PROGRESS.name(),
                ExamAttemptStatus.TIMEOUT.name());
        List<ExamAttempt> attempts = examAttemptRepository.findActiveAttemptsForUpdate(examId, userId, statuses);
        return attempts.isEmpty() ? null : attempts.getFirst();
    }

    private void validateAccessCode(ExamSessionAssignment assignment, String accessCode) {
        if (!StringUtils.hasText(assignment.getAccessCode())) {
            return;
        }
        if (!StringUtils.hasText(accessCode)) {
            throw new ApiException(ErrorCode.E221, "accessCode is required");
        }
        String normalized = accessCode.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new ApiException(ErrorCode.E221, "accessCode is required");
        }
        if (!assignment.getAccessCode().equals(normalized)) {
            throw new ApiException(ErrorCode.E221, "accessCode is invalid");
        }
    }

    private Exam loadExamOrThrow(String examId) {
        // Validate exam existence + enabled flag
        Exam exam = examRepository.findByIdAndDeletedFalse(examId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, ErrorCode.E227.message("Exam not found")));
        if (!exam.isEnabled()) {
            throw new ApiException(ErrorCode.E427);
        }
        return exam;
    }

    private ExamVersion loadPublishedVersionOrThrow(Exam exam) {
        // Validate published pointer + published status
        String publishedId = exam.getPublishedExamVersionId();
        if (publishedId == null || publishedId.isBlank()) {
            throw new ApiException(ErrorCode.E428);
        }
        ExamVersion published = examVersionRepository.findByIdAndExamIdAndDeletedFalse(publishedId, exam.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.E428));
        if (!ExamVersionStatus.PUBLISHED.name().equalsIgnoreCase(published.getStatus())) {
            throw new ApiException(ErrorCode.E431);
        }
        return published;
    }
}
