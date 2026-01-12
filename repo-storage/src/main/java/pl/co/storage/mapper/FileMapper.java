package pl.co.storage.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.co.storage.dto.FileResponse;
import pl.co.storage.entity.File;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileMapper {
    @Mapping(target = "fileId", source = "id")
    FileResponse toResponse(File file);

    List<FileResponse> toResponseList(List<File> files);
}
