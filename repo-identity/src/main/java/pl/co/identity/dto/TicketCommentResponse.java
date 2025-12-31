package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TicketCommentResponse {
    private final String id;
    private final String ticketId;
    private final String authorId;
    private final String content;
    private final Instant createdAt;
}
