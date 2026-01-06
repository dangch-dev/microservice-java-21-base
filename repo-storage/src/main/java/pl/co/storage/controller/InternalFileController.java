package pl.co.storage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.common.dto.ApiResponse;
import pl.co.storage.dto.FileResponse;
import pl.co.storage.service.FileService;

@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_INTERNAL.name())")
public class InternalFileController {

    private final FileService fileService;

    @PostMapping("/{id}/commit")
    public ApiResponse<FileResponse> commit(@PathVariable("id") String id) {
        return ApiResponse.ok(fileService.commit(id));
    }
}
