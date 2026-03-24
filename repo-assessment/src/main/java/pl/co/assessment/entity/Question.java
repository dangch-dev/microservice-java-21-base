package pl.co.assessment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.co.common.jpa.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_questions_deleted", columnList = "deleted")
})
public class Question extends BaseEntity {
}
