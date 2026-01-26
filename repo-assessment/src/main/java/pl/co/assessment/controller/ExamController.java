package pl.co.assessment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pl.co.assessment.dto.*;
import pl.co.assessment.service.ExamService;
import pl.co.common.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_ADMIN.name())")
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ApiResponse<ExamCreateResponse> create(@Valid @RequestBody ExamCreateRequest request) {
        return ApiResponse.ok(examService.create(request));
    }

    @GetMapping
    public ApiResponse<ExamPageResponse> list(@RequestParam(value = "searchValue", required = false) String searchValue,
                                              @RequestParam(value = "categoryId", required = false) String categoryId,
                                              @RequestParam(value = "page", required = false) Integer page,
                                              @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.ok(examService.list(searchValue, categoryId, page, size));
    }

    @PutMapping("/{examId}/edit")
    public ApiResponse<ExamEditorResponse> requestEdit(@PathVariable("examId") String examId) {
        return ApiResponse.ok(examService.requestEdit(examId));
    }

    @PostMapping("/{examId}/draft/save")
    public ApiResponse<Void> saveDraft(@PathVariable("examId") String examId,
                                       @Valid @RequestBody ExamDraftSaveRequest request) {
        examService.saveDraft(examId, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{examId}/draft/discard")
    public ApiResponse<Void> discardDraft(@PathVariable("examId") String examId) {
        examService.discardDraft(examId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{examId}/draft/publish")
    public ApiResponse<Void> publishDraft(@PathVariable("examId") String examId) {
        examService.publishDraft(examId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{examId}/enable")
    public ApiResponse<Void> updateStatus(@PathVariable("examId") String examId,
                                          @RequestBody ExamStatusUpdateRequest request) {
        examService.updateStatus(examId, request);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{examId}")
    public ApiResponse<Void> delete(@PathVariable("examId") String examId) {
        examService.deleteExam(examId);
        return ApiResponse.ok(null);
    }
}
