package pl.co.identity.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserLookupSearchRequest {
    private String searchValue;
    private List<String> roleNames;
    @Min(value = 0, message = "Input Parameter Error. Invalid data value. (page)")
    private Integer page = 0;
    @Min(value = 1, message = "Input Parameter Error. Invalid data value. (size)")
    private Integer size = 20;
}
