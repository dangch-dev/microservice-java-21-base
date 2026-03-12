package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.co.assessment.entity.ExamSessionTargetType;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ExamSessionResponse {
    private final String id;
    private final String examId;
    private final String title;
    private final Instant startAt;
    private final Instant endAt;
    private final ExamSessionTargetType targetType;
    private final String code;
    private final String accessCode;
    private final Integer assignmentCount;
}
