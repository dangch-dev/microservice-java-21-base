package pl.co.storage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pl.co.storage.entity.OwnerType;

import java.util.List;

public record FilePresignRequest(
        @NotNull OwnerType ownerType,
        @NotBlank String ownerId,
        @NotEmpty List<@Valid FileUploadRequest> files
) {
}
