package pl.co.common.file;

public record FileMeta(
        String fileId,
        String filename,
        String mimeType,
        Long sizeBytes
) {
}
