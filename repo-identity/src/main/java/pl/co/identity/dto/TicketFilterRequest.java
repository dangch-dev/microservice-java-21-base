package pl.co.identity.dto;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class TicketFilterRequest {
    private Integer page = 0;
    private Integer size = 20;
    private String status;
}
