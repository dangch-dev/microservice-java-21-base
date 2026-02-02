package pl.co.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import jakarta.validation.Valid;
import pl.co.assessment.dto.AttemptManagementPageResponse;
import pl.co.assessment.dto.AttemptLockResponse;
import pl.co.assessment.dto.AttemptManualGradingSaveRequest;
import pl.co.assessment.dto.AttemptResultResponse;
import pl.co.assessment.service.AttemptService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;
import pl.co.common.security.SecurityConstants;

import java.time.Instant;

@RestController
@RequestMapping("/management/attempts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_ADMIN.name())")
public class ManagementAttemptController {

    private final AttemptService attemptService;

    @GetMapping
    public ApiResponse<AttemptManagementPageResponse> list(@RequestParam(value = "status", required = false) String status,
                                                           @RequestParam(value = "gradingStatus", required = false) String gradingStatus,
                                                           @RequestParam(value = "examId", required = false) String examId,
                                                           @RequestParam(value = "userId", required = false) String userId,
                                                           @RequestParam(value = "from", required = false)
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                           @RequestParam(value = "to", required = false)
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                           @RequestParam(value = "page", required = false) Integer page,
                                                           @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.ok(attemptService.listManagementAttempts(status, gradingStatus, examId, userId, from, to, page, size));
    }

    @GetMapping("/{attemptId}/result")
    public ApiResponse<AttemptResultResponse> getResult(@PathVariable("attemptId") String attemptId) {
        return ApiResponse.ok(attemptService.getManagementAttemptResult(attemptId));
    }

    @GetMapping("/{attemptId}/manual-grading")
    public ApiResponse<AttemptResultResponse> getManualGrading(@PathVariable("attemptId") String attemptId,
                                                               @RequestHeader(SecurityConstants.HEADER_SESSION_ID) String sessionId,
                                                               Authentication authentication) {
        // Resolve admin identity and open manual grading session (acquire/renew lock).
        String adminId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(attemptService.getManagementAttemptManualGrading(attemptId, adminId, sessionId));
    }

    @PostMapping("/{attemptId}/manual-grading/heartbeat")
    public ApiResponse<AttemptLockResponse> heartbeat(@PathVariable("attemptId") String attemptId,
                                                      @RequestHeader(SecurityConstants.HEADER_SESSION_ID) String sessionId,
                                                      Authentication authentication) {
        // Renew grading lock to keep the session alive.
        String adminId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(attemptService.heartbeatManualGrading(attemptId, adminId, sessionId));
    }

    @PutMapping("/{attemptId}/manual-grading")
    public ApiResponse<AttemptLockResponse> saveManualGrading(@PathVariable("attemptId") String attemptId,
                                                              @RequestHeader(SecurityConstants.HEADER_SESSION_ID) String sessionId,
                                                              @Valid @RequestBody AttemptManualGradingSaveRequest request,
                                                              Authentication authentication) {
        // Save manual grading for selected questions; lock must be valid.
        String adminId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(attemptService.saveManualGrading(attemptId, adminId, sessionId, request));
    }
}
