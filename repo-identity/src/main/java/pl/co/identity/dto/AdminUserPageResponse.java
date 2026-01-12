package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminUserPageResponse {
    private final List<AdminUserResponse> items;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;
}
