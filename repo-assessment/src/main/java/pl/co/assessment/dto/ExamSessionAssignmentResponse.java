package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExamSessionAssignmentResponse {
    private final String id;
    private final String sessionId;
    private final String userId;
    private final String code;
    private final String accessCode;
    private final String attemptId;
}

