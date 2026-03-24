package pl.co.assessment.service;

import pl.co.assessment.dto.AttemptLockResponse;

public interface ManualGradingLockService {
    AttemptLockResponse acquire(String attemptId, String ownerId, String sessionId);
    AttemptLockResponse renew(String attemptId, String ownerId, String sessionId);
    AttemptLockResponse validate(String attemptId, String ownerId, String sessionId);
}
