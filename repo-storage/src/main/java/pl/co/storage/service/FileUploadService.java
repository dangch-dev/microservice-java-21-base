package pl.co.storage.service;

import org.springframework.web.multipart.MultipartFile;
import pl.co.common.file.FileMeta;

public interface FileUploadService {
    FileMeta upload(MultipartFile file);
}
