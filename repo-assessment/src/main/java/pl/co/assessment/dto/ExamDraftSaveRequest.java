package pl.co.assessment.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.Valid;

import java.util.List;

@Getter
@Setter
public class ExamDraftSaveRequest {
    @Valid
    private ExamDraftMetadataRequest metadata;
    @Valid
    private List<ExamDraftChangeRequest> questionChanges;
}
