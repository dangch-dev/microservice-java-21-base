package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AttemptResultResponse {
    private final String attemptId;
    private final String examId;
    private final String examVersionId;
    private final String status;
    private final String gradingStatus;
    private final String name;
    private final String description;
    private final Integer durationMinutes;
    private final Instant startTime;
    private final Instant endTime;
    private final BigDecimal score;
    private final BigDecimal maxScore;
    private final BigDecimal percent;
    private final List<AttemptResultItemResponse> items;
}
