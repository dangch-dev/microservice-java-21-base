package pl.co.auth.dto;

import lombok.Builder;

@Builder
public record ResetPasswordValidateResponse(
        String email,
        String fullName,
        String avatarUrl
) {
}
