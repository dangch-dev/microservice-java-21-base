package pl.co.assessment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.co.assessment.dto.AttemptAnswerSaveRequest;
import pl.co.assessment.dto.AttemptDetailResponse;
import pl.co.assessment.dto.AttemptStartResponse;
import pl.co.assessment.service.AttemptAnswerService;
import pl.co.assessment.service.AttemptQueryService;
import pl.co.assessment.service.AttemptStartService;
import pl.co.assessment.service.AttemptServiceImpl;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;

@RestController
@RequestMapping("/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptAnswerService attemptAnswerService;
    private final AttemptQueryService attemptQueryService;
    private final AttemptStartService attemptStartService;
    private final AttemptServiceImpl attemptServiceImpl;

    @PostMapping("/exam/{examId}/start")
    public ApiResponse<AttemptStartResponse> start(@PathVariable("examId") @NotBlank String examId,
                                                   Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(attemptStartService.startAttempt(examId, userId));
    }

    @GetMapping("/{attemptId}")
    public ApiResponse<AttemptDetailResponse> getAttempt(@PathVariable("attemptId") String attemptId,
                                                         Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(attemptQueryService.getAttempt(attemptId, userId));
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
        attemptServiceImpl.submit(attemptId, userId);
        return ApiResponse.ok(null);
    }
}
