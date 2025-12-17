package pl.co.identity.dto;

import pl.co.identity.entity.UserStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AdminUpdateUserRequest {
    private UserStatus status;

    private Set<String> roles;

    @Size(max = 120)
    private String fullName;

    @Size(max = 30)
    private String phoneNumber;

    @Size(max = 255)
    private String avatarUrl;

    @Size(max = 255)
    private String address;
}
