package pl.co.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserLookupRequest {
    private List<@NotBlank String> userIds;
}
