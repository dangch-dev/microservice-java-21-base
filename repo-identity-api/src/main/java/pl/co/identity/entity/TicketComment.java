package pl.co.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "ticket_comment")
public class TicketComment extends BaseEntity {

    @Column(nullable = false, length = 26)
    private String ticketId;

    @Column(nullable = false, length = 26)
    private String authorId;

    @Column(nullable = false, length = 2000)
    private String content;
}
