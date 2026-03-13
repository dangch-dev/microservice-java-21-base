package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GetSessionResponse {
    private final String examId;
    private final String examVersionId;
    private final String name;
    private final String description;
    private final Integer durationMinutes;
    private final boolean requiredAccessCode;
    private final String sessionAttemptId;
    private final String sessionAttemptStatus;
    private final Instant startTime;
    private final Instant endTime;

    private final String activeAttemptId;
}
