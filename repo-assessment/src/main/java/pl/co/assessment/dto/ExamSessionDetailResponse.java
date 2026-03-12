package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.co.assessment.entity.ExamSessionTargetType;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class ExamSessionDetailResponse {
    private final String id;
    private final String examId;
    private final String examName;
    private final String examDescription;
    private final String title;
    private final Instant startAt;
    private final Instant endAt;
    private final ExamSessionTargetType targetType;
    private final String code;
    private final String accessCode;
    private final List<ExamSessionAssignmentDetailResponse> assignments;
}
