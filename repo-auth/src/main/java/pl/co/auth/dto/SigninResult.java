package pl.co.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SigninResult {
    private final TokenResponse tokens;
    private final boolean emailVerified;
}
