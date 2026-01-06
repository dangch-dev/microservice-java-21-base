package pl.co.storage.dto;

import pl.co.storage.entity.FileStatus;
import pl.co.storage.entity.OwnerType;

import java.time.Instant;

public record FileResponse(
        String id,
        OwnerType ownerType,
        String ownerId,
        String objectKey,
        String filename,
        String mimeType,
        Long sizeBytes,
        FileStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
