package pl.co.assessment.service;

import pl.co.assessment.dto.AttemptStartResponse;
import pl.co.assessment.dto.GetSessionResponse;

public interface ExamSessionService {
    GetSessionResponse getSession(String code, String userId);

    AttemptStartResponse startSession(String code, String userId, String accessCode);
}
