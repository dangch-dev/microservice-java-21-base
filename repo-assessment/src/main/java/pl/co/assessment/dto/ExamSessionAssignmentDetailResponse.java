package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ExamSessionAssignmentDetailResponse {
    private final String id;
    private final String sessionId;
    private final String userId;
    private final String code;
    private final String accessCode;
    private final String attemptId;
    private final String attemptStatus;
    private final String gradingStatus;
    private final BigDecimal score;
    private final BigDecimal maxScore;
    private final BigDecimal percent;
    private final String userFullName;
    private final String userEmail;
    private final String userPhoneNumber;
}
