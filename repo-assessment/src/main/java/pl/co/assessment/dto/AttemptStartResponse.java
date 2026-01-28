package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AttemptStartResponse {
    private final String attemptId;
    private final AttemptStartMode mode;
    private final String examId;
    private final String examVersionId;
    private final String status;
    private final String name;
    private final String description;
    private final Instant startTime;
    private final Integer durationMinutes;
    private final Long timeRemainingSeconds;
}
