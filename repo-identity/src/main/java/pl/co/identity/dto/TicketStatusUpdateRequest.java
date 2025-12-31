package pl.co.identity.dto;

import lombok.Getter;
import lombok.Setter;
import pl.co.identity.entity.TicketStatus;

@Getter
@Setter
public class TicketStatusUpdateRequest {
    private TicketStatus status;

    /**
     * Optional: admin can assign ticket to a user id.
     */
    private String assignedTo;
}
