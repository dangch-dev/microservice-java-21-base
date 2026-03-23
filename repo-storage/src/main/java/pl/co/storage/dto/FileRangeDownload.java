package pl.co.storage.dto;

import java.io.InputStream;

public record FileRangeDownload(
        InputStream stream,
        String contentType,
        long rangeLength,
        long totalSize
) {
}
