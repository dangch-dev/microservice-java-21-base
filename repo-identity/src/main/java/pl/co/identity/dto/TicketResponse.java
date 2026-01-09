package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;
import pl.co.common.file.FileMeta;
import pl.co.identity.entity.TicketStatus;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class TicketResponse {
    private final String id;
    private final String title;
    private final String description;
    private final TicketStatus status;
    private final String createdBy;
    private final String assignedTo;
    private final List<FileMeta> files;
    private final Instant createdAt;
    private final Instant updatedAt;
}
