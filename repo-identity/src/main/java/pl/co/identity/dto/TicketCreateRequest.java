package pl.co.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pl.co.common.file.FileMeta;

import java.util.List;

@Getter
@Setter
public class TicketCreateRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 4000)
    private String description;

    private List<FileMeta> files;
}
