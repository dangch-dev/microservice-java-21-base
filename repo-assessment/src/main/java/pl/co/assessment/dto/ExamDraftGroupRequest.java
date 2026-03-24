package pl.co.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pl.co.assessment.entity.json.GroupPromptContent;
import pl.co.common.anotation.Trim;

import java.util.List;

@Getter
@Setter
public class ExamDraftGroupRequest {
    @NotBlank
    @Trim
    private String groupId;

    @NotNull
    @Valid
    private GroupPromptContent promptContent;

    private List<@NotBlank String> questionIds;
}
