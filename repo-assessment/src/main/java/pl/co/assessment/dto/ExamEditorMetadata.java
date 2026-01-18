package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExamEditorMetadata {
    private final String name;
    private final String description;
    private final Integer durationMinutes;
    private final boolean shuffleQuestions;
    private final boolean shuffleOptions;
}
