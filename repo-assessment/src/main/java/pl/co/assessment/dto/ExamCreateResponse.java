package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExamCreateResponse {
    private final String examId;
    private final String examVersionId;
}
