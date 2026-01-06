package pl.co.storage.mapper;

import org.mapstruct.Mapper;
import pl.co.storage.dto.FileResponse;
import pl.co.storage.entity.File;
import pl.co.storage.entity.FileStatus;
import pl.co.storage.entity.OwnerType;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileMapper {
    FileResponse toResponse(File file);

    List<FileResponse> toResponseList(List<File> files);

    default OwnerType toOwnerType(String value) {
        return value == null ? null : OwnerType.valueOf(value);
    }

    default FileStatus toFileStatus(String value) {
        return value == null ? null : FileStatus.valueOf(value);
    }
}
