package pl.co.storage.dto;

public record FileResponse(
        String fileId,
        String filename,
        String mimeType,
        Long sizeBytes
) {
}
