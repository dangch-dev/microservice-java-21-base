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
@Table(name = "taggables", indexes = {
        @Index(name = "uk_taggables_tag_taggable_deleted",
                columnList = "tag_id, taggable_id, taggable_type, deleted", unique = true),
        @Index(name = "idx_taggables_tag_id_deleted", columnList = "tag_id, deleted"),
        @Index(name = "idx_taggables_taggable_type_deleted", columnList = "taggable_type, deleted"),
        @Index(name = "idx_taggables_taggable_id_type_deleted",
                columnList = "taggable_id, taggable_type, deleted"),
        @Index(name = "idx_taggables_deleted", columnList = "deleted")
})
public class Taggable extends BaseEntity {

    @Column(name = "tag_id", nullable = false, length = 26)
    private String tagId;

    @Column(name = "taggable_id", nullable = false, length = 26)
    private String taggableId;

    @Column(name = "taggable_type", nullable = false, length = 50)
    private String taggableType;
}
