package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, String> {
}
