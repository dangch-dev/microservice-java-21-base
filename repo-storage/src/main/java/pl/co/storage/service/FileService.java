package pl.co.storage.service;

import org.springframework.web.multipart.MultipartFile;
import pl.co.common.file.FileMeta;
import pl.co.storage.dto.FileDownload;

public interface FileService {

    FileMeta upload(MultipartFile file);

    FileDownload download(String fileId);

    void commit(String fileId);

    void cleanupPending();
}
