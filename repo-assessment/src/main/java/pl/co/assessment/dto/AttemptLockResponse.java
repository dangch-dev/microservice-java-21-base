package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttemptLockResponse {
    private final String ownerId;
    private final String sessionId;
    private final Long ttlSeconds;
}
