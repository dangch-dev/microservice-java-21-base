package pl.co.storage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.filter.principal.AuthPrincipal;
import pl.co.storage.dto.FilePresignRequest;
import pl.co.storage.dto.FilePresignResponse;
import pl.co.storage.dto.FileResponse;
import pl.co.storage.entity.OwnerType;
import pl.co.storage.service.FileService;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/presign")
    public ApiResponse<List<FilePresignResponse>> presign(@Valid @RequestBody FilePresignRequest request,
                                                          Authentication authentication) {
        String userId = extractUserId(authentication);
        return ApiResponse.ok(fileService.presign(request, userId));
    }

    @GetMapping
    public ApiResponse<List<FileResponse>> list(@RequestParam(value = "ownerType", required = false) OwnerType ownerType,
                                                @RequestParam(value = "ownerId", required = false) String ownerId,
                                                @RequestParam(value = "ids", required = false) List<String> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            return ApiResponse.ok(fileService.listByIds(ids));
        }
        if (ownerType == null || !StringUtils.hasText(ownerId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "ownerType and ownerId are required when ids are not provided");
        }
        return ApiResponse.ok(fileService.listByOwner(ownerType, ownerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<FileResponse> getOne(@PathVariable("id") String id) {
        List<FileResponse> responses = fileService.listByIds(List.of(id));
        if (responses.isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "File not found");
        }
        return ApiResponse.ok(responses.getFirst());
    }

    private String extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal authPrincipal) {
            return authPrincipal.userId();
        }
        return null;
    }
}
