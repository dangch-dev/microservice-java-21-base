package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TicketPageResponse {
    private final List<TicketResponse> items;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;
}
