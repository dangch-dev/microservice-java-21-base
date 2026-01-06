package pl.co.storage.service.impl;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.util.UlidGenerator;
import pl.co.storage.config.StorageProperties;
import pl.co.storage.dto.FilePresignRequest;
import pl.co.storage.dto.FilePresignResponse;
import pl.co.storage.dto.FileResponse;
import pl.co.storage.dto.FileUploadRequest;
import pl.co.storage.entity.File;
import pl.co.storage.entity.FileStatus;
import pl.co.storage.entity.OwnerType;
import pl.co.storage.mapper.FileMapper;
import pl.co.storage.repository.FileRepository;
import pl.co.storage.service.FileService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_PRESIGN_SECONDS = Duration.ofDays(7).toSeconds();

    private final FileRepository fileRepository;
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public List<FilePresignResponse> presign(FilePresignRequest request, String createdBy) {
        if (request.files() == null || request.files().isEmpty()) {
            throw new ApiException(ErrorCode.E219, "No files to presign");
        }

        List<File> files = new ArrayList<>(request.files().size());
        String ownerId = request.ownerId().trim();
        for (FileUploadRequest file : request.files()) {
            String fileId = UlidGenerator.nextUlid();
            String objectKey = buildObjectKey(request.ownerType(), ownerId, fileId, file.filename());
            File entity = new File();
            entity.setId(fileId);
            entity.setOwnerType(request.ownerType().name());
            entity.setOwnerId(ownerId);
            entity.setObjectKey(objectKey);
            entity.setFilename(file.filename());
            entity.setMimeType(file.mimeType());
            entity.setSizeBytes(file.sizeBytes());
            entity.setStatus(FileStatus.PENDING.name());
            files.add(entity);
        }

        List<File> saved = fileRepository.saveAll(files);
        return saved.stream()
                .map(file -> new FilePresignResponse(file.getId(), file.getObjectKey(),
                        generatePresignedPutUrl(file.getObjectKey(), file.getMimeType())))
                .toList();
    }

    @Override
    @Transactional
    public FileResponse commit(String fileId) {
        File file = fileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "File not found"));
        if (FileStatus.DELETED.name().equals(file.getStatus())) {
            throw new ApiException(ErrorCode.NOT_FOUND, "File deleted");
        }

        Optional<StatObjectResponse> statResponse = statObject(file);
        if (statResponse.isEmpty()) {
            throw new ApiException(ErrorCode.E227, "Uploaded object not found");
        }
        StatObjectResponse stat = statResponse.get();
        if (file.getSizeBytes() == null) {
            file.setSizeBytes(stat.size());
        }
        file.setStatus(FileStatus.READY.name());
        File saved = fileRepository.save(file);
        return fileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> listByOwner(OwnerType ownerType, String ownerId) {
        List<File> files = fileRepository
                .findByOwnerTypeAndOwnerIdAndStatusAndDeletedFalseOrderByCreatedAtAsc(
                        ownerType.name(), ownerId, FileStatus.READY.name());
        return fileMapper.toResponseList(files);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> listByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<File> files = fileRepository.findByIdInAndStatusAndDeletedFalse(ids, FileStatus.READY.name());
        return fileMapper.toResponseList(files);
    }


    @Override
    @Transactional
    public void cleanupPending() {
        Instant threshold = Instant.now().minus(storageProperties.getPendingTtl());
        List<File> expired = fileRepository
                .findByStatusAndUpdatedAtBeforeAndDeletedFalse(FileStatus.PENDING.name(), threshold);
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(file -> {
            if (storageProperties.isDeleteObjectOnSoftDelete()) {
                removeObjectQuietly(file.getObjectKey());
            }
        });
        fileRepository.deleteAll(expired);
    }

    private String generatePresignedPutUrl(String objectKey, String mimeType) {
        try {
            long ttlSeconds = Math.min(storageProperties.getPresignTtl().toSeconds(), MAX_PRESIGN_SECONDS);
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(objectKey)
                    .method(Method.PUT)
                    .expiry((int) ttlSeconds, TimeUnit.SECONDS)
                    .extraQueryParams(mimeType != null ? Map.of("Content-Type", mimeType) : Collections.emptyMap())
                    .build());
        } catch (Exception ex) {
            log.error("Failed to create presigned URL for object {}: {}", objectKey, ex.getMessage());
            throw new ApiException(ErrorCode.E281, "Unable to create presign URL", ex);
        }
    }

    private Optional<StatObjectResponse> statObject(File file) {
        try {
            StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(file.getObjectKey())
                    .build());
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("Object not found for file {}: {}", file.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    private void removeObjectQuietly(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to remove object {} from MinIO: {}", objectKey, ex.getMessage());
        }
    }

    private String buildObjectKey(OwnerType ownerType, String ownerId, String fileId, String filename) {
        String safeOwnerId = ownerId == null ? "unknown" : ownerId.trim();
        String sanitizedFileName = sanitizeFileName(filename);
        return ownerType.name().toLowerCase(Locale.ROOT) + "/" + safeOwnerId + "/" + fileId + "/" + sanitizedFileName;
    }

    private String sanitizeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
