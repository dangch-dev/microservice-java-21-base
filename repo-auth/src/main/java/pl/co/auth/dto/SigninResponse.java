package pl.co.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SigninResponse {
    private final boolean emailVerified;
}
