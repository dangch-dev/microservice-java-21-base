package pl.co.common.jwt.record;

import java.util.List;

// Use for verify information in token's claim
public record JwtVerificationOptions(List<String> audience,
                                     String type) {
}
