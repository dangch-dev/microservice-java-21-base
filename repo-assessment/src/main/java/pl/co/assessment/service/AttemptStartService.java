package pl.co.assessment.service;

import pl.co.assessment.dto.AttemptStartResponse;

public interface AttemptStartService {
    AttemptStartResponse startAttempt(String examId, String userId);
}
