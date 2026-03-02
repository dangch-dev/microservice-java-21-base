package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttemptLockResponse {
    private final String ownerId;
    private final String ownerFullName;
    private final String ownerAvatarUrl;
    private final String ownerEmail;
    private final String ownerRoleName;
    private final String sessionId;
    private final Long ttlSeconds;
}
