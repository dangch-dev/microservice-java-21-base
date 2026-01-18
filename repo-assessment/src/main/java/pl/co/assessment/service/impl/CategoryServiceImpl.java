package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.co.assessment.dto.CategoryResponse;
import pl.co.assessment.repository.CategoryRepository;
import pl.co.assessment.service.CategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getDescription()
                ))
                .toList();
    }
}
