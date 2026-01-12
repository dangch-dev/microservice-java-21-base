package pl.co.storage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.co.common.dto.ApiResponse;
import pl.co.storage.dto.FileDownload;
import pl.co.storage.dto.FileResponse;
import pl.co.storage.service.FileService;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileService.upload(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getOne(@PathVariable("id") String id,
                                           @RequestParam(value = "disposition", required = false, defaultValue = "inline")
                                           String disposition) {
        FileDownload download = fileService.download(id);
        String contentType = download.contentType() == null || download.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : download.contentType();
        String filename = download.filename() == null ? "file" : download.filename();
        String safeDisposition = "attachment".equalsIgnoreCase(disposition) ? "attachment" : "inline";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, safeDisposition + "; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.sizeBytes() == null ? -1 : download.sizeBytes())
                .body(new InputStreamResource(download.stream()));
    }
}
