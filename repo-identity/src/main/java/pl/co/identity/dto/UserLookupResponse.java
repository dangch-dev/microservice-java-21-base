package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserLookupResponse {
    private final String userId;
    private final String fullName;
    private final String avatarUrl;
    private final String email;
    private final String phoneNumber;
    private final List<String> roleNames;
}
