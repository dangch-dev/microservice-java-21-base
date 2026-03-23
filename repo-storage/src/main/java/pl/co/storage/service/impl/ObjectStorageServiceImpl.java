package pl.co.storage.service.impl;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.co.storage.config.StorageProperties;
import pl.co.storage.service.ObjectStorageService;

import java.io.InputStream;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectStorageServiceImpl implements ObjectStorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @Override
    public Optional<StatObjectResponse> statObject(String objectKey) {
        try {
            StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("Object not found for key {}: {}", objectKey, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public InputStream getObject(String objectKey) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(storageProperties.getMinio().getBucket())
                .object(objectKey)
                .build());
    }

    @Override
    public InputStream getObjectRange(String objectKey, long offset, long length) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(storageProperties.getMinio().getBucket())
                .object(objectKey)
                .offset(offset)
                .length(length)
                .build());
    }

    @Override
    public void putObject(String objectKey, InputStream stream, long size, String contentType) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(storageProperties.getMinio().getBucket())
                .object(objectKey)
                .stream(stream, size, -1)
                .contentType(contentType)
                .build());
    }

    @Override
    public void removeObjectQuietly(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to remove object {} from MinIO: {}", objectKey, ex.getMessage());
        }
    }
}
