package pl.co.assessment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import pl.co.assessment.dto.AttemptAnswerSaveRequest;
import pl.co.assessment.dto.AttemptDetailResponse;
import pl.co.assessment.dto.AttemptPageResponse;
import pl.co.assessment.dto.AttemptResultResponse;
import pl.co.assessment.dto.AttemptStartResponse;
import pl.co.assessment.service.AttemptAnswerService;
import pl.co.assessment.service.AttemptService;
import pl.co.assessment.service.AttemptStartService;
import pl.co.assessment.service.AttemptSubmissionService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;
import pl.co.common.security.RoleName;
import java.time.Instant;

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
        boolean isGuest = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> RoleName.ROLE_GUEST.name().equals(a.getAuthority()));
        return ApiResponse.ok(attemptStartService.startAttempt(examId, userId, isGuest));
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
        String userId = AuthUtils.resolveUserId(authentication);
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
