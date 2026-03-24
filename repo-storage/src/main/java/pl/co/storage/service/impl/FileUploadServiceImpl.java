package pl.co.storage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.co.common.event.EventPublisher;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.file.FileMeta;
import pl.co.common.util.StringUtils;
import pl.co.common.util.UlidGenerator;
import pl.co.storage.dto.FileHlsRequest;
import pl.co.storage.entity.File;
import pl.co.storage.entity.FileStatus;
import pl.co.storage.mapper.FileMapper;
import pl.co.storage.repository.FileRepository;
import pl.co.storage.service.FileUploadService;
import pl.co.storage.service.ObjectStorageService;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileRepository fileRepository;
    private final ObjectStorageService objectStorageService;
    private final FileMapper fileMapper;
    private final EventPublisher eventPublisher;

    @Value("${kafka.topics.file-hls}")
    private String fileHlsTopic;

    @Override
    public FileMeta upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.E243, "File is required");
        }
        String fileId = UlidGenerator.nextUlid();
        String originalName = file.getOriginalFilename();
        String objectKey = buildObjectKey(fileId, originalName);
        String mimeType = file.getContentType();

        try (InputStream inputStream = file.getInputStream()) {
            objectStorageService.putObject(objectKey, inputStream, file.getSize(), mimeType);
        } catch (Exception ex) {
            log.error("Failed to upload object {}: {}", objectKey, ex.getMessage());
            throw new ApiException(ErrorCode.E281, "Unable to upload file", ex);
        }

        File entity = new File();
        entity.setId(fileId);
        entity.setObjectKey(objectKey);
        entity.setFilename(originalName == null || originalName.isBlank() ? "file" : originalName);
        entity.setMimeType(mimeType == null ? "application/octet-stream" : mimeType);
        entity.setSizeBytes(file.getSize());
        entity.setStatus(FileStatus.PENDING.name());
        File saved = fileRepository.save(entity);
        if (shouldPublishHls(saved)) {
            FileHlsRequest request = new FileHlsRequest();
            request.setFileId(saved.getId());
            eventPublisher.publish(fileHlsTopic, saved.getId(), request);
        }
        return fileMapper.toResponse(saved);
    }

    private String buildObjectKey(String fileId, String filename) {
        String sanitizedFileName = sanitizeFileName(filename);
        return "files/" + fileId + "/" + sanitizedFileName;
    }

    private String sanitizeFileName(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }
        return filename.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.toLowerCase().startsWith("video/");
    }

    private boolean shouldPublishHls(File file) {
        return file != null
                && isVideo(file.getMimeType())
                && StringUtils.hasText(fileHlsTopic);
    }
}
