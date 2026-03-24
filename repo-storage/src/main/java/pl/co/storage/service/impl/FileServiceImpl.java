package pl.co.storage.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.storage.config.StorageProperties;
import pl.co.storage.entity.FileStatus;
import pl.co.storage.repository.FileRepository;
import pl.co.storage.service.FileService;
import pl.co.storage.service.ObjectStorageService;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final StorageProperties storageProperties;
    private final ObjectStorageService objectStorageService;

    @Override
    @Transactional
    public void commit(String fileId) {
        var file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "File not found"));

        var statResponse = objectStorageService.statObject(file.getObjectKey());
        if (statResponse.isEmpty()) {
            throw new ApiException(ErrorCode.E227, "Uploaded object not found");
        }
        var stat = statResponse.get();
        if (file.getSizeBytes() == null) {
            file.setSizeBytes(stat.size());
        }
        if (FileStatus.PENDING.name().equalsIgnoreCase(file.getStatus())) {
            file.setStatus(FileStatus.READY.name());
        }
        fileRepository.save(file);
    }

    @Override
    @Transactional
    public void cleanupPending() {
        Instant threshold = Instant.now().minus(storageProperties.getPendingTtl());
        List<pl.co.storage.entity.File> expired = fileRepository
                .findByStatusAndUpdatedAtBeforeAndDeletedFalse(FileStatus.PENDING.name(), threshold);
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(file -> {
            if (storageProperties.isDeleteObjectOnSoftDelete()) {
                objectStorageService.removeObjectQuietly(file.getObjectKey());
            }
        });
        fileRepository.deleteAll(expired);
    }
}
