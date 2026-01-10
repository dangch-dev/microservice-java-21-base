package pl.co.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.co.common.file.FileMeta;
import pl.co.common.jpa.BaseEntity;
import pl.co.common.file.FileMetaConverter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ticket")
public class Ticket extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 26)
    private String createdBy;

    @Column(length = 26)
    private String assignedTo;

    @Convert(converter = FileMetaConverter.class)
    @Column(columnDefinition = "json")
    private List<FileMeta> files;
}
