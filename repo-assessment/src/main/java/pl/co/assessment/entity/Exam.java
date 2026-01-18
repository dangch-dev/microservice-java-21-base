package pl.co.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.co.common.jpa.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exams", indexes = {
        @Index(name = "idx_exams_category_id", columnList = "category_id"),
        @Index(name = "idx_exams_published_exam_version_id", columnList = "published_exam_version_id"),
        @Index(name = "idx_exams_draft_exam_version_id", columnList = "draft_exam_version_id"),
        @Index(name = "idx_exams_deleted", columnList = "deleted")
})
public class Exam extends BaseEntity {

    @Column(name = "category_id", length = 26)
    private String categoryId;

    @Column(name = "published_exam_version_id", length = 26)
    private String publishedExamVersionId;

    @Column(name = "draft_exam_version_id", length = 26)
    private String draftExamVersionId;
}
