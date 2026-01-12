package pl.co.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pl.co.common.file.FileMeta;

import java.util.List;

@Getter
@Setter
public class TicketCommentRequest {
    @NotBlank
    @Size(max = 2000)
    private String content;

    private List<FileMeta> files;
}
