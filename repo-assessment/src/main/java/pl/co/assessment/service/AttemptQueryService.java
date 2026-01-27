package pl.co.assessment.service;

import pl.co.assessment.dto.AttemptDetailResponse;
import pl.co.assessment.dto.AttemptPageResponse;
import pl.co.assessment.dto.AttemptResultResponse;

import java.time.Instant;

public interface AttemptQueryService {
    AttemptDetailResponse getAttempt(String attemptId, String userId);
    AttemptPageResponse listAttempts(String userId,
                                     String status,
                                     String gradingStatus,
                                     Instant fromTime,
                                     Instant toTime,
                                     Integer page,
                                     Integer size);
    AttemptResultResponse getAttemptResult(String attemptId, String userId);
}
