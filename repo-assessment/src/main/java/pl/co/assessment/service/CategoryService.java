package pl.co.assessment.service;

import pl.co.assessment.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> list();
}
