package pl.co.assessment.service;

import pl.co.assessment.dto.AttemptAnswerSaveRequest;

public interface AttemptAnswerService {
    void saveAnswers(String attemptId, String userId, AttemptAnswerSaveRequest request);
}
