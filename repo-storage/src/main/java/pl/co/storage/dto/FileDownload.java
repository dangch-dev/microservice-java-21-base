package pl.co.storage.dto;

import java.io.InputStream;

public record FileDownload(
        InputStream stream,
        String filename,
        String contentType,
        Long sizeBytes
) {
}
