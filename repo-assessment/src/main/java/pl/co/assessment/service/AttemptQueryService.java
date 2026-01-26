package pl.co.assessment.service;

import pl.co.assessment.dto.AttemptDetailResponse;

public interface AttemptQueryService {
    AttemptDetailResponse getAttempt(String attemptId, String userId);
}
