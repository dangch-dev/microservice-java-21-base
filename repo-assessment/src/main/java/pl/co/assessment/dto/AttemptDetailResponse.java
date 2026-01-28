package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AttemptDetailResponse {
    private final String attemptId;
    private final String examId;
    private final String examVersionId;
    private final String status;
    private final String name;
    private final String description;
    private final Integer durationMinutes;
    private final Instant startTime;
    private final Long timeRemainingSeconds;
    private final List<AttemptQuestionResponse> questions;
    private final List<AttemptAnswerResponse> answers;
}
