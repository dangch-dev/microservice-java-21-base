package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserLookupRequest {
    private final List<String> userIds;
}
