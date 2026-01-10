package pl.co.storage.service;

import org.springframework.web.multipart.MultipartFile;
import pl.co.storage.dto.FileDownload;
import pl.co.storage.dto.FileResponse;

public interface FileService {

    FileResponse upload(MultipartFile file);

    FileDownload download(String fileId);

    FileResponse commit(String fileId);

    void cleanupPending();
}
