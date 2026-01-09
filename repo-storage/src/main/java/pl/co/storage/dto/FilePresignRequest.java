package pl.co.storage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record FilePresignRequest(
        @NotEmpty List<@Valid FileUploadRequest> files
) {
}
