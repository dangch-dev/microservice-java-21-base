package pl.co.storage.dto;

public record FilePresignResponse(
        String fileId,
        String objectKey,
        String presignUrl
) {
}
