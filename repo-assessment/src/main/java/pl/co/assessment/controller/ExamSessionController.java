package pl.co.assessment.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.assessment.dto.AttemptStartResponse;
import pl.co.assessment.dto.GetSessionResponse;
import pl.co.assessment.dto.StartSessionRequest;
import pl.co.assessment.service.ExamSessionService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    @GetMapping("/{code}")
    public ApiResponse<GetSessionResponse> getSession(@PathVariable("code") @NotBlank String code,
                                                      Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(examSessionService.getSession(code, userId));
    }

    @PostMapping("/{code}/start")
    public ApiResponse<AttemptStartResponse> startSession(@PathVariable("code") @NotBlank String code,
                                                          @RequestBody(required = false) StartSessionRequest request,
                                                          Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        String accessCode = request == null ? null : request.getAccessCode();
        return ApiResponse.ok(examSessionService.startSession(code, userId, accessCode));
    }

}
