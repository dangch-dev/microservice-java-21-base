package pl.co.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record FileUploadRequest(
        @NotBlank String filename,
        @NotBlank String mimeType,
        @Positive Long sizeBytes
) {
}
