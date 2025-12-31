package pl.co.identity.dto;

import lombok.Getter;
import lombok.Setter;
import pl.co.identity.entity.TicketStatus;

@Getter
@Setter
public class TicketFilterRequest {
    private int page = 0;
    private int size = 20;
    private TicketStatus status;
}
