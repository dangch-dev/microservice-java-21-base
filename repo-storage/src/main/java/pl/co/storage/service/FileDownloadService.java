package pl.co.storage.service;

import pl.co.storage.dto.FileDownload;
import pl.co.storage.dto.FileRangeDownload;

public interface FileDownloadService {
    FileDownload download(String fileId);

    FileRangeDownload downloadRange(String fileId, long start, Long end);
}
