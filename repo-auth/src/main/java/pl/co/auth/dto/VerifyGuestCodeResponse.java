package pl.co.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class VerifyGuestCodeResponse {
    private boolean valid;
    private String sessionId;
    private String assignmentId;
    private String examId;
    private Instant startAt;
    private Instant endAt;
    private String accessCode;
    private String userId;
}
