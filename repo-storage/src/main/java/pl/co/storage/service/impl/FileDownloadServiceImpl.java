package pl.co.storage.service.impl;

import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.storage.dto.FileDownload;
import pl.co.storage.dto.FileRangeDownload;
import pl.co.storage.entity.File;
import pl.co.storage.repository.FileRepository;
import pl.co.storage.service.FileDownloadService;
import pl.co.storage.service.ObjectStorageService;

import java.io.InputStream;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadServiceImpl implements FileDownloadService {

    private final FileRepository fileRepository;
    private final ObjectStorageService objectStorageService;

    @Override
    public FileDownload download(String fileId) {
        File file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "File not found"));

        Optional<StatObjectResponse> statResponse = objectStorageService.statObject(file.getObjectKey());
        if (statResponse.isEmpty()) {
            throw new ApiException(ErrorCode.E227, "File object not found");
        }

        try {
            InputStream stream = objectStorageService.getObject(file.getObjectKey());
            StatObjectResponse stat = statResponse.get();
            return new FileDownload(
                    stream,
                    file.getFilename(),
                    file.getMimeType(),
                    stat.size());
        } catch (Exception ex) {
            log.error("Failed to get object {}: {}", file.getObjectKey(), ex.getMessage());
            throw new ApiException(ErrorCode.E281, "Unable to download file", ex);
        }
    }

    @Override
    public FileRangeDownload downloadRange(String fileId, long start, Long end) {
        File file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "File not found"));

        Optional<StatObjectResponse> statResponse = objectStorageService.statObject(file.getObjectKey());
        if (statResponse.isEmpty()) {
            throw new ApiException(ErrorCode.E227, "File object not found");
        }
        long totalSize = statResponse.get().size();
        if (totalSize <= 0) {
            throw new ApiException(ErrorCode.E281, "Unable to read file size");
        }

        long safeStart = start;
        Long safeEnd = end;
        if (start < 0) {
            long suffix = Math.abs(start);
            if (suffix <= 0) {
                throw new ApiException(ErrorCode.E221, "Invalid range");
            }
            safeStart = Math.max(0, totalSize - suffix);
            safeEnd = totalSize - 1;
        }
        if (safeEnd == null) {
            safeEnd = totalSize - 1;
        } else {
            safeEnd = Math.min(safeEnd, totalSize - 1);
        }
        if (safeStart > safeEnd || safeStart >= totalSize) {
            throw new ApiException(ErrorCode.E221, "Invalid range");
        }
        long length = safeEnd - safeStart + 1;

        try {
            InputStream stream = objectStorageService.getObjectRange(file.getObjectKey(), safeStart, length);
            return new FileRangeDownload(
                    stream,
                    file.getMimeType(),
                    length,
                    totalSize
            );
        } catch (Exception ex) {
            log.error("Failed to get object range {} ({}-{}): {}", file.getObjectKey(), safeStart, safeEnd, ex.getMessage());
            throw new ApiException(ErrorCode.E281, "Unable to download file range", ex);
        }
    }
}
