package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.co.assessment.entity.json.GroupPromptContent;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuestionGroupResponse {
    private final String groupId;
    private final String groupVersionId;
    private final GroupPromptContent promptContent;
    private final List<String> questionIds;
}
