package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AttemptPageResponse {
    private final List<AttemptListItemResponse> items;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;
}
