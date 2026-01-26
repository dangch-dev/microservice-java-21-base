package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExamEditorResponse {
    private final ExamEditorMetadata metadata;
    private final List<ExamEditorQuestion> questions;
}
