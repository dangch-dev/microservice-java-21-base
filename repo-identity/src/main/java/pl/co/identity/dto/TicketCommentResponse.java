package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;
import pl.co.common.file.FileMeta;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class TicketCommentResponse {
    private final String id;
    private final String ticketId;
    private final String authorId;
    private final String content;
    private final List<FileMeta> files;
    private final Instant createdAt;
}
