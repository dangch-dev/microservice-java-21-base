package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExamEditorResponse {
    private final String mode;
    private final String examVersionId;
    private final ExamEditorMetadata metadata;
    private final List<ExamEditorQuestion> questions;
}
