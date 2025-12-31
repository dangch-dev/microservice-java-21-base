package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;
import pl.co.identity.entity.TicketStatus;

import java.time.Instant;

@Getter
@Builder
public class TicketResponse {
    private final String id;
    private final String title;
    private final String description;
    private final TicketStatus status;
    private final String createdBy;
    private final String assignedTo;
    private final Instant createdAt;
    private final Instant updatedAt;
}
