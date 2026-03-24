package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExamPageResponse {
    private final List<ExamListItemResponse> items;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;
}
