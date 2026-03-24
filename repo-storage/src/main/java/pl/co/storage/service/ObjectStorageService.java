package pl.co.storage.service;

import io.minio.StatObjectResponse;

import java.io.InputStream;
import java.util.Optional;

public interface ObjectStorageService {
    Optional<StatObjectResponse> statObject(String objectKey);

    InputStream getObject(String objectKey) throws Exception;

    InputStream getObjectRange(String objectKey, long offset, long length) throws Exception;

    void putObject(String objectKey, InputStream stream, long size, String contentType) throws Exception;

    void removeObjectQuietly(String objectKey);
}
