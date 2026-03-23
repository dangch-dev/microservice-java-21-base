package pl.co.storage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.co.common.dto.ApiResponse;
import pl.co.common.file.FileMeta;
import pl.co.storage.dto.FileDownload;
import pl.co.storage.dto.FileRangeDownload;
import pl.co.storage.service.FileDownloadService;
import pl.co.storage.service.FileHlsService;
import pl.co.storage.service.FileUploadService;
import pl.co.common.util.StringUtils;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileDownloadService fileDownloadService;
    private final FileHlsService fileHlsService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileMeta> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileUploadService.upload(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getOne(@PathVariable("id") String id,
                                           @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
                                           @RequestParam(value = "disposition", required = false, defaultValue = "inline")
                                           String disposition) {
        if (StringUtils.hasText(rangeHeader)) {
            return handleRangeRequest(id, rangeHeader);
        }
        FileDownload download = fileDownloadService.download(id);
        String contentType = download.contentType() == null || download.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : download.contentType();
        String filename = download.filename() == null ? "file" : download.filename();
        String safeDisposition = "attachment".equalsIgnoreCase(disposition) ? "attachment" : "inline";
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, safeDisposition + "; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.sizeBytes() == null ? -1 : download.sizeBytes())
                .body(new InputStreamResource(download.stream()));
    }

    @GetMapping("/{id}/hls/index.m3u8")
    public ResponseEntity<Resource> getHlsPlaylist(@PathVariable("id") String id) {
        FileDownload download = fileHlsService.downloadHlsPlaylist(id);
        if (download == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        String contentType = download.contentType() == null || download.contentType().isBlank()
                ? "application/vnd.apple.mpegurl"
                : download.contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.sizeBytes() == null ? -1 : download.sizeBytes())
                .body(new InputStreamResource(download.stream()));
    }

    @GetMapping("/{id}/hls/{segment}")
    public ResponseEntity<Resource> getHlsSegment(@PathVariable("id") String id,
                                                  @PathVariable("segment") String segment) {
        if (!StringUtils.hasText(segment) || segment.contains("/") || segment.contains("..")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        FileDownload download = fileHlsService.downloadHlsSegment(id, segment);
        if (download == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        String contentType = download.contentType() == null || download.contentType().isBlank()
                ? "video/mp2t"
                : download.contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.sizeBytes() == null ? -1 : download.sizeBytes())
                .body(new InputStreamResource(download.stream()));
    }

    private ResponseEntity<Resource> handleRangeRequest(String id, String rangeHeader) {
        RangeRequest range = parseRange(rangeHeader);
        if (range == null) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
        FileRangeDownload download = fileDownloadService.downloadRange(id, range.start(), range.end());
        String contentType = download.contentType() == null || download.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : download.contentType();
        long totalSize = download.totalSize();
        long start = range.start();
        Long end = range.end();
        if (start < 0) {
            long suffix = Math.abs(start);
            start = Math.max(0, totalSize - suffix);
            end = totalSize - 1;
        } else if (end == null) {
            end = start + download.rangeLength() - 1;
        } else {
            end = Math.min(end, totalSize - 1);
        }
        String contentRange = "bytes " + start + "-" + end + "/" + download.totalSize();
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, contentRange)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.rangeLength())
                .body(new InputStreamResource(download.stream()));
    }

    private RangeRequest parseRange(String rangeHeader) {
        // Expect: bytes=start-end | bytes=start- | bytes=-suffix
        if (!StringUtils.hasText(rangeHeader) || !rangeHeader.startsWith("bytes=")) {
            return null;
        }
        String value = rangeHeader.substring("bytes=".length()).trim();
        int commaIndex = value.indexOf(',');
        if (commaIndex >= 0) {
            value = value.substring(0, commaIndex).trim();
        }
        int dashIndex = value.indexOf('-');
        if (dashIndex < 0) {
            return null;
        }
        String startPart = value.substring(0, dashIndex).trim();
        String endPart = value.substring(dashIndex + 1).trim();
        try {
            if (startPart.isEmpty()) {
                // suffix range: bytes=-N (handled later in service as invalid start)
                long suffix = Long.parseLong(endPart);
                if (suffix <= 0) {
                    return null;
                }
                return new RangeRequest(-suffix, null);
            }
            long start = Long.parseLong(startPart);
            Long end = endPart.isEmpty() ? null : Long.parseLong(endPart);
            if (start < 0 || (end != null && end < start)) {
                return null;
            }
            return new RangeRequest(start, end);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record RangeRequest(long start, Long end) {
    }
}
