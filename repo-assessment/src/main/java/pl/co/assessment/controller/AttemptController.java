package pl.co.assessment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import pl.co.assessment.dto.*;
import pl.co.assessment.service.AttemptAnswerService;
import pl.co.assessment.service.AttemptService;
import pl.co.assessment.service.AttemptStartService;
import pl.co.assessment.service.AttemptSubmissionService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;
import pl.co.common.security.RoleName;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptAnswerService attemptAnswerService;
    private final AttemptService attemptService;
    private final AttemptStartService attemptStartService;
    private final AttemptSubmissionService attemptSubmissionService;

    @PostMapping("/exam/{examId}/start")
    public ApiResponse<AttemptStartResponse> start(@PathVariable("examId") @NotBlank String examId,
                                                   Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        boolean isGuest = false;
        if (authentication != null && authentication.getAuthorities() != null) {
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(a -> a == null ? null : a.getAuthority())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            isGuest = roles.size() == 1 && roles.contains(RoleName.ROLE_GUEST.name());
        }
        return ApiResponse.ok(attemptStartService.startAttempt(examId, userId, isGuest, true));
    }

    @GetMapping("/{attemptId}")
    public ApiResponse<AttemptDetailResponse> getAttempt(@PathVariable("attemptId") String attemptId,
                                                         Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(attemptService.getAttempt(attemptId, userId));
    }

    @GetMapping("/{attemptId}/result")
    public ApiResponse<AttemptResultResponse> getAttemptResult(@PathVariable("attemptId") String attemptId,
                                                               Authentication authentication) {
        String userId = authentication == null ? null : AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(attemptService.getAttemptResult(attemptId, userId));
    }

    @GetMapping
    public ApiResponse<AttemptPageResponse> listAttempts(@RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "gradingStatus", required = false) String gradingStatus,
                                                         @RequestParam(value = "from", required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                         @RequestParam(value = "to", required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                         @RequestParam(value = "page", required = false) Integer page,
                                                         @RequestParam(value = "size", required = false) Integer size,
                                                         Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(attemptService.listAttempts(userId, status, gradingStatus, from, to, page, size));
    }

    @PutMapping("/{attemptId}/answers")
    public ApiResponse<Void> saveAnswers(@PathVariable("attemptId") String attemptId,
                                         @Valid @RequestBody AttemptAnswerSaveRequest request,
                                         Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        attemptAnswerService.saveAnswers(attemptId, userId, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{attemptId}/submit")
    public ApiResponse<Void> submit(@PathVariable("attemptId") String attemptId,
                                    Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        attemptSubmissionService.submit(attemptId, userId);
        return ApiResponse.ok(null);
    }

}
