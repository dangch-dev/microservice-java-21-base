package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExamListItemResponse {
    private final String examId;
    private final String examVersionId;
    private final String categoryName;
    private final String name;
    private final String description;
    private final String status;
    private final Integer durationMinutes;
    private final boolean shuffleQuestions;
    private final boolean shuffleOptions;
    private final boolean enabled;
}
