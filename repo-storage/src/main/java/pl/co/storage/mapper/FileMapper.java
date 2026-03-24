package pl.co.storage.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.co.common.file.FileMeta;
import pl.co.storage.entity.File;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileMapper {
    @Mapping(target = "fileId", source = "id")
    FileMeta toResponse(File file);

    List<FileMeta> toResponseList(List<File> files);
}
