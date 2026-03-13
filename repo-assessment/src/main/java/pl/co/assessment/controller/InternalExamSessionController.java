package pl.co.assessment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.assessment.dto.VerifyGuestCodeRequest;
import pl.co.assessment.dto.VerifyGuestCodeResponse;
import pl.co.assessment.service.ManagementExamSessionService;
import pl.co.common.dto.ApiResponse;

@RestController
@RequestMapping("/internal/exam-sessions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_INTERNAL.name())")
public class InternalExamSessionController {

    private final ManagementExamSessionService managementExamSessionService;

    @PostMapping("/verify-guest-code")
    public ApiResponse<VerifyGuestCodeResponse> verifyGuestCode(@Valid @RequestBody VerifyGuestCodeRequest request) {
        return ApiResponse.ok(managementExamSessionService.verifyGuestCode(request.getCode()));
    }
}
