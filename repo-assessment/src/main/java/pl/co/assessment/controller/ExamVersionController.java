package pl.co.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.assessment.dto.ExamEditorResponse;
import pl.co.assessment.service.ExamVersionService;
import pl.co.common.dto.ApiResponse;

@RestController
@RequestMapping("/management/exam-versions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_ADMIN.name())")
public class ExamVersionController {

    private final ExamVersionService examVersionService;

    @GetMapping("/{examVersionId}/preview")
    public ApiResponse<ExamEditorResponse> preview(@PathVariable("examVersionId") String examVersionId) {
        return ApiResponse.ok(examVersionService.previewVersion(examVersionId));
    }
}
