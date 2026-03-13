package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class VerifyGuestCodeResponse {
    private final boolean valid;
    private final String sessionId;
    private final String assignmentId;
    private final String examId;
    private final Instant startAt;
    private final Instant endAt;
    private final String userId;
}
