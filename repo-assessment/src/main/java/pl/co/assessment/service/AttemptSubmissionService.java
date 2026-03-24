package pl.co.assessment.service;

import pl.co.assessment.entity.ExamAttempt;

import java.util.List;

public interface AttemptSubmissionService {
    void submit(String attemptId, String userId);
    void timeout(List<ExamAttempt> timeOutAttempts);
    void finalizeTimeouts();
}
