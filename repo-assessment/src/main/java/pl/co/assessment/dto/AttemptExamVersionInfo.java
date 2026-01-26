package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttemptExamVersionInfo {
    private final String id;
    private final String name;
    private final Integer durationMinutes;
    private final Boolean shuffleQuestions;
    private final Boolean shuffleOptions;
}
