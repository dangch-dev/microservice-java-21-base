package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExamFormImportResponse {
    private String examId;
    private String draftExamVersionId;
    private int importedCount;
    private int skippedCount;
    private List<String> warnings;
}
