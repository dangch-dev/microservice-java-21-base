package pl.co.assessment.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import pl.co.common.anotation.Trim;

@Getter
@Setter
public class ExamFormImportRequest {
    @Trim
    private String formId;
    @Trim
    private String formName;
    @Trim
    private String formUrl;
    private JsonNode form;
}
