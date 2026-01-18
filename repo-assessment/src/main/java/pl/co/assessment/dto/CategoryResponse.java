package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryResponse {
    private final String id;
    private final String name;
    private final String description;
}
