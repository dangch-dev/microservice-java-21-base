package pl.co.storage.service;

import pl.co.storage.dto.FileDownload;

public interface FileHlsService {
    FileDownload downloadHlsPlaylist(String fileId);

    FileDownload downloadHlsSegment(String fileId, String segmentName);

    void processHls(String fileId);
}
