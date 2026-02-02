package pl.co.assessment.service;

import pl.co.assessment.dto.AttemptDetailResponse;
import pl.co.assessment.dto.AttemptLockResponse;
import pl.co.assessment.dto.AttemptManagementPageResponse;
import pl.co.assessment.dto.AttemptManualGradingSaveRequest;
import pl.co.assessment.dto.AttemptPageResponse;
import pl.co.assessment.dto.AttemptResultResponse;

import java.time.Instant;

public interface AttemptService {
    AttemptDetailResponse getAttempt(String attemptId, String userId);
    AttemptPageResponse listAttempts(String userId,
                                     String status,
                                     String gradingStatus,
                                     Instant fromTime,
                                     Instant toTime,
                                     Integer page,
                                     Integer size);
    AttemptResultResponse getAttemptResult(String attemptId, String userId);

    AttemptManagementPageResponse listManagementAttempts(String status,
                                                         String gradingStatus,
                                                         String examId,
                                                         String userId,
                                                         Instant fromTime,
                                                         Instant toTime,
                                                         Integer page,
                                                         Integer size);

    AttemptResultResponse getManagementAttemptResult(String attemptId);

    AttemptResultResponse getManagementAttemptManualGrading(String attemptId, String adminId, String sessionId);
    AttemptLockResponse heartbeatManualGrading(String attemptId, String adminId, String sessionId);
    AttemptLockResponse saveManualGrading(String attemptId,
                                          String adminId,
                                          String sessionId,
                                          AttemptManualGradingSaveRequest request);
}
