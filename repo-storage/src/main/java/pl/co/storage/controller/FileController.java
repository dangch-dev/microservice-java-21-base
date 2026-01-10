package pl.co.storage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.filter.principal.AuthPrincipal;
import pl.co.storage.dto.FileResponse;
import pl.co.storage.dto.FileDownload;
import pl.co.storage.service.FileService;

import java.util.List;

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
                                           @RequestParam(value = "disposition", required = false, defaultValue = "attachment")
                                           String disposition) {
        FileDownload download = fileService.download(id);
        String contentType = download.contentType() == null || download.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : download.contentType();
        String filename = download.filename() == null ? "file" : download.filename();
        String safeDisposition = "inline".equalsIgnoreCase(disposition) ? "inline" : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, safeDisposition + "; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.sizeBytes() == null ? -1 : download.sizeBytes())
                .body(new InputStreamResource(download.stream()));
    }
}
