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
@Table(name = "tags", indexes = {
        @Index(name = "uk_tags_name_deleted", columnList = "name, deleted", unique = true),
        @Index(name = "uk_tags_slug_deleted", columnList = "slug, deleted", unique = true),
        @Index(name = "idx_tags_deleted", columnList = "deleted")
})
public class Tag extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;
}
