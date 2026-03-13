package pl.co.assessment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.co.assessment.dto.ExamSessionAssignmentRequest;
import pl.co.assessment.dto.ExamSessionAssignmentResponse;
import pl.co.assessment.dto.ExamSessionCreateRequest;
import pl.co.assessment.dto.ExamSessionDetailResponse;
import pl.co.assessment.dto.ExamSessionListItemResponse;
import pl.co.assessment.dto.ExamSessionResponse;
import pl.co.assessment.dto.ExamSessionUpdateRequest;
import pl.co.assessment.service.ManagementExamSessionService;
import pl.co.common.dto.ApiResponse;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_ADMIN.name())")
public class ManagementExamSessionController {

    private final ManagementExamSessionService managementExamSessionService;

    @PostMapping("/exams/{examId}/sessions")
    public ApiResponse<ExamSessionResponse> createSession(@PathVariable("examId") String examId,
                                                          @Valid @RequestBody ExamSessionCreateRequest request) {
        return ApiResponse.ok(managementExamSessionService.createSession(examId, request));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ExamSessionListItemResponse>> listSessions(@RequestParam(value = "examId", required = false) String examId,
                                                                       @RequestParam(value = "startTime", required = false) Instant startTime,
                                                                       @RequestParam(value = "endTime", required = false) Instant endTime,
                                                                       @RequestParam(value = "searchValue", required = false) String searchValue,
                                                                       @RequestParam(value = "page", required = false) Integer page,
                                                                       @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.ok(managementExamSessionService.listSessions(examId, startTime, endTime, searchValue, page, size));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<ExamSessionDetailResponse> getSession(@PathVariable("sessionId") String sessionId) {
        return ApiResponse.ok(managementExamSessionService.getSession(sessionId));
    }

    @PutMapping("/sessions/{sessionId}")
    public ApiResponse<ExamSessionResponse> updateSession(@PathVariable("sessionId") String sessionId,
                                                          @Valid @RequestBody ExamSessionUpdateRequest request) {
        return ApiResponse.ok(managementExamSessionService.updateSession(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/rotate-code")
    public ApiResponse<ExamSessionResponse> rotateSessionCode(@PathVariable("sessionId") String sessionId) {
        return ApiResponse.ok(managementExamSessionService.rotateSessionCode(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable("sessionId") String sessionId) {
        managementExamSessionService.deleteSession(sessionId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/sessions/{sessionId}/assignments")
    public ApiResponse<List<ExamSessionAssignmentResponse>> addAssignments(@PathVariable("sessionId") String sessionId,
                                                                           @Valid @RequestBody ExamSessionAssignmentRequest request) {
        return ApiResponse.ok(managementExamSessionService.addAssignments(sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}/assignments/{assignmentId}")
    public ApiResponse<Void> deleteAssignment(@PathVariable("sessionId") String sessionId,
                                              @PathVariable("assignmentId") String assignmentId) {
        managementExamSessionService.deleteAssignment(sessionId, assignmentId);
        return ApiResponse.ok(null);
    }
}
