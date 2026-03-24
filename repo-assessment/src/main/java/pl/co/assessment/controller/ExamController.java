package pl.co.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.co.assessment.dto.ExamPageResponse;
import pl.co.assessment.service.ExamService;
import pl.co.common.dto.ApiResponse;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping
    public ApiResponse<ExamPageResponse> list(@RequestParam(value = "searchValue", required = false) String searchValue,
                                              @RequestParam(value = "page", required = false) Integer page,
                                              @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.ok(examService.listPublic(searchValue, page, size));
    }
}
