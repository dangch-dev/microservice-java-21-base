package pl.co.storage.service;

import pl.co.storage.dto.FilePresignRequest;
import pl.co.storage.dto.FilePresignResponse;
import pl.co.storage.dto.FileResponse;
import pl.co.storage.entity.OwnerType;

import java.util.List;

public interface FileService {
    List<FilePresignResponse> presign(FilePresignRequest request, String createdBy);

    FileResponse commit(String fileId);

    List<FileResponse> listByOwner(OwnerType ownerType, String ownerId);

    List<FileResponse> listByIds(List<String> ids);

    void cleanupPending();
}
