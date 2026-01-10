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
@Table(name = "ticket_comment")
public class TicketComment extends BaseEntity {

    @Column(nullable = false, length = 26)
    private String ticketId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Convert(converter = FileMetaConverter.class)
    @Column(columnDefinition = "json")
    private List<FileMeta> files;
}
