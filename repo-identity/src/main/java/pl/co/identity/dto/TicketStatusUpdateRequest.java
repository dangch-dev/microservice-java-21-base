package pl.co.identity.dto;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class TicketStatusUpdateRequest {
    private String status;

    /**
     * Optional: admin can assign ticket to a user id.
     */
    private String assignedTo;
}
