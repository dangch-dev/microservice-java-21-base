package pl.co.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.notification.NotificationAction;
import pl.co.common.file.FileMeta;
import pl.co.common.file.FilePublisher;
import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.identity.dto.TicketCreateRequest;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;
import pl.co.common.notification.NotificationEvent;
import pl.co.common.notification.NotificationPublisher;
import pl.co.identity.entity.TicketComment;
import pl.co.identity.entity.Ticket;
import pl.co.identity.entity.TicketStatus;
import pl.co.identity.mapper.TicketMapper;
import pl.co.identity.repository.TicketCommentRepository;
import pl.co.identity.repository.TicketRepository;
import pl.co.identity.repository.UserRepository;
import pl.co.identity.service.TicketUserService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketUserServiceImpl implements TicketUserService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;
    private final FilePublisher filePublisher;

    @Override
    @Transactional
    public TicketResponse create(String userId, TicketCreateRequest request) {
        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.OPEN.name())
                .createdBy(userId)
                .assignedTo(null)
                .files(request.getFiles())
                .build();
        Ticket saved = ticketRepository.save(ticket);
        filePublisher.publish(request.getFiles());
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketPageResponse list(String userId, TicketFilterRequest filter) {
        int pageValue = filter.getPage() == null ? 0 : filter.getPage();
        int sizeValue = filter.getSize() == null ? 20 : filter.getSize();
        PageRequest page = PageRequest.of(Math.max(pageValue, 0), Math.max(sizeValue, 1));
        Specification<Ticket> spec = buildSpec(userId, filter);
        Page<Ticket> result = ticketRepository.findAll(spec, page);
        List<TicketResponse> items = result.getContent().stream().map(this::toListResponse).toList();
        return TicketPageResponse.builder()
                .items(items)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse get(String userId, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        enforceAccess(userId, ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse cancel(String userId, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        if (!userId.equals(ticket.getCreatedBy())) {
            throw new ApiException(ErrorCode.E230, "Only creator can cancel");
        }
        if (TicketStatus.COMPLETED.name().equals(ticket.getStatus())
                || TicketStatus.CANCELLED.name().equals(ticket.getStatus())) {
            throw new ApiException(ErrorCode.E221, "Ticket cannot be cancelled");
        }
        ticket.setStatus(TicketStatus.CANCELLED.name());
        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponse addComment(String userId, String ticketId, TicketCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        enforceCommentAccess(userId, ticket);
        TicketComment comment = TicketComment.builder()
                .ticketId(ticketId)
                .content(request.getContent())
                .files(request.getFiles())
                .build();
        TicketComment saved = ticketCommentRepository.save(comment);
        //Publish file
        filePublisher.publish(request.getFiles());
        // Notification
        Map<String, Object> payload = baseTicketPayload(ticket);
        payload.put("commentId", comment.getId());
        payload.put("comment", comment.getContent());

        notificationPublisher.publish(new NotificationEvent(
                userId,
                NotificationAction.TICKET_COMMENT_ADDED.name(),
                NotificationAction.TICKET_COMMENT_ADDED.title(),
                NotificationAction.TICKET_COMMENT_ADDED.message(ticket.getTitle()),
                ticket.getId(),
                payload,
                "ticket:" + ticket.getId() + ":comment:" + comment.getId() + ":user:" + userId
        ));
        return toCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCommentResponse> listComments(String userId, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        enforceCommentAccess(userId, ticket);
        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    private String validateTicketStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(status).name();
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.E204, "Invalid ticket status: " + status);
        }
    }

    private Specification<Ticket> buildSpec(String userId, TicketFilterRequest filter) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("createdBy"), userId));
            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), validateTicketStatus(filter.getStatus())));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void enforceAccess(String userId, Ticket ticket) {
        if (!ticket.getCreatedBy().equals(userId)) {
            throw new ApiException(ErrorCode.E230, "No authority");
        }
    }

    private void enforceCommentAccess(String userId, Ticket ticket) {
        if (!ticket.getCreatedBy().equals(userId) &&
                (ticket.getAssignedTo() == null || !userId.equals(ticket.getAssignedTo()))) {
            throw new ApiException(ErrorCode.E230, "No authority to comment");
        }
    }

    private TicketCommentResponse toCommentResponse(TicketComment comment) {
        var author = comment.getCreatedBy() == null ? null : userRepository.findById(comment.getCreatedBy()).orElse(null);
        return TicketCommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicketId())
                .createdBy(comment.getCreatedBy())
                .creatorName(author == null ? null : author.getFullName())
                .creatorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .content(comment.getContent())
                .files(comment.getFiles())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private TicketResponse toListResponse(Ticket ticket) {
        TicketStatus status = ticket.getStatus() == null ? null : TicketStatus.valueOf(ticket.getStatus());
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(status)
                .createdBy(null)
                .creatorName(null)
                .creatorAvatarUrl(null)
                .assignedTo(ticket.getAssignedTo())
                .files(ticket.getFiles())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private Map<String, Object> baseTicketPayload(Ticket ticket) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("status", ticket.getStatus());
        payload.put("title", ticket.getTitle());
        return payload;
    }
}
