package pl.co.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pl.co.identity.entity.TicketComment;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, String>, JpaSpecificationExecutor<TicketComment> {
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(String ticketId);
}
