package pl.co.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.security.AuthPrincipal;
import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.identity.dto.TicketCreateRequest;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;
import pl.co.common.notification.NotificationEvent;
import pl.co.common.notification.NotificationPublisher;
import pl.co.common.notification.ResourceType;
import pl.co.identity.entity.TicketComment;
import pl.co.identity.entity.Ticket;
import pl.co.identity.entity.TicketStatus;
import pl.co.identity.mapper.TicketMapper;
import pl.co.identity.repository.TicketCommentRepository;
import pl.co.identity.repository.TicketRepository;
import pl.co.identity.repository.UserRepository;
import pl.co.identity.service.TicketService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public TicketResponse create(AuthPrincipal principal, TicketCreateRequest request) {
        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.OPEN)
                .createdBy(principal.userId())
                .assignedTo(null)
                .build();
        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketPageResponse list(AuthPrincipal principal, TicketFilterRequest filter) {
        PageRequest page = PageRequest.of(Math.max(filter.getPage(), 0), Math.max(filter.getSize(), 1));
        Specification<Ticket> spec = buildSpec(principal, filter);
        Page<Ticket> result = ticketRepository.findAll(spec, page);
        List<TicketResponse> items = result.getContent().stream().map(ticketMapper::toResponse).toList();
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
    public TicketResponse get(AuthPrincipal principal, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        enforceAccess(principal, ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateStatus(AuthPrincipal principal, String ticketId, TicketStatusUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        TicketStatus previousStatus = ticket.getStatus();
        String previousAssignee = ticket.getAssignedTo();
        String newAssigneeName = null;
        // Assign only by admin
        if (request.getAssignedTo() != null && !request.getAssignedTo().isBlank()) {
            if (!principal.hasRole("ROLE_ADMIN")) {
                throw new ApiException(ErrorCode.E230, "No authority to assign");
            }
            var user = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ApiException(ErrorCode.E227, "Assignee not found"));
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
            if (!isAdmin) {
                throw new ApiException(ErrorCode.E230, "Assignee must be an admin");
            }
            ticket.setAssignedTo(request.getAssignedTo());
            newAssigneeName = user.getFullName();
        }
        // Status change only by creator or assignee (after assignment if any)
        if (request.getStatus() != null) {
            if (!principal.userId().equals(ticket.getCreatedBy()) &&
                    (ticket.getAssignedTo() == null || !principal.userId().equals(ticket.getAssignedTo()))) {
                throw new ApiException(ErrorCode.E230, "No authority to change status");
            }
            ticket.setStatus(request.getStatus());
        }
        Ticket saved = ticketRepository.save(ticket);
        notifyAssignmentAndStatus(principal, saved, previousStatus, previousAssignee, newAssigneeName);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse cancel(AuthPrincipal principal, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        if (!principal.userId().equals(ticket.getCreatedBy())) {
            throw new ApiException(ErrorCode.E230, "Only creator can cancel");
        }
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ApiException(ErrorCode.E221, "Ticket cannot be cancelled");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket saved = ticketRepository.save(ticket);
        notifyCancel(principal, saved);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponse addComment(AuthPrincipal principal, String ticketId, TicketCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        enforceCommentAccess(principal, ticket);
        TicketComment comment = TicketComment.builder()
                .ticketId(ticketId)
                .authorId(principal.userId())
                .content(request.getContent())
                .build();
        TicketComment saved = ticketCommentRepository.save(comment);
        notifyComment(principal, ticket, saved);
        return toCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCommentResponse> listComments(AuthPrincipal principal, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        enforceCommentAccess(principal, ticket);
        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    private Specification<Ticket> buildSpec(AuthPrincipal principal, TicketFilterRequest filter) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!principal.hasRole("ROLE_ADMIN")) {
                predicates.add(cb.equal(root.get("createdBy"), principal.userId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void enforceAccess(AuthPrincipal principal, Ticket ticket) {
        if (principal.hasRole("ROLE_ADMIN")) {
            return;
        }
        if (!ticket.getCreatedBy().equals(principal.userId())) {
            throw new ApiException(ErrorCode.E230, "No authority");
        }
    }

    private void enforceCommentAccess(AuthPrincipal principal, Ticket ticket) {
        if (principal.hasRole("ROLE_ADMIN")) {
            // admin can comment only if assigned or creator? requirement says creator or assignee
            // so fall through to same rule
        }
        if (!ticket.getCreatedBy().equals(principal.userId()) &&
                (ticket.getAssignedTo() == null || !principal.userId().equals(ticket.getAssignedTo()))) {
            throw new ApiException(ErrorCode.E230, "No authority to comment");
        }
    }

    private TicketCommentResponse toCommentResponse(TicketComment comment) {
        return TicketCommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicketId())
                .authorId(comment.getAuthorId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private void notifyAssignmentAndStatus(AuthPrincipal actor, Ticket ticket, TicketStatus previousStatus, String previousAssignee, String assigneeName) {
        if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().equals(previousAssignee)) {
            String assigneeId = ticket.getAssignedTo();
            String creatorId = ticket.getCreatedBy();
            Map<String, Object> payload = baseTicketPayload(ticket, actor.userId());
            payload.put("assigneeName", assigneeName);
            sendNotificationIfTarget(actor.userId(), assigneeId,
                    "ticket.assigned",
                    "Ticket assigned",
                    "Ticket '" + ticket.getTitle() + "' assigned to you",
                    payload,
                    "ticket:" + ticket.getId() + ":assigned:" + assigneeId + ":user:" + assigneeId);
            if (!creatorId.equals(assigneeId)) {
                sendNotificationIfTarget(actor.userId(), creatorId,
                        "ticket.assigned",
                        "Ticket assigned",
                        "Ticket '" + ticket.getTitle() + "' assigned to " + (assigneeName != null ? assigneeName : assigneeId),
                        payload,
                        "ticket:" + ticket.getId() + ":assigned:" + assigneeId + ":user:" + creatorId);
            }
        }
        if (previousStatus != ticket.getStatus()) {
            notifyStatusChange(actor, ticket, previousStatus);
        }
    }

    private void notifyStatusChange(AuthPrincipal actor, Ticket ticket, TicketStatus previousStatus) {
        Map<String, Object> payload = baseTicketPayload(ticket, actor.userId());
        payload.put("previousStatus", previousStatus);
        Set<String> targets = new HashSet<>();
        targets.add(ticket.getCreatedBy());
        if (ticket.getAssignedTo() != null) {
            targets.add(ticket.getAssignedTo());
        }
        for (String target : targets) {
            sendNotificationIfTarget(actor.userId(), target,
                    "ticket.status",
                    "Ticket status changed",
                    "Ticket '" + ticket.getTitle() + "' changed to " + ticket.getStatus(),
                    payload,
                    "ticket:" + ticket.getId() + ":status:" + ticket.getStatus() + ":user:" + target);


            // TODO: Call API for send notification

        }
    }

    private void notifyCancel(AuthPrincipal actor, Ticket ticket) {
        if (ticket.getAssignedTo() == null) {
            return;
        }
        Map<String, Object> payload = baseTicketPayload(ticket, actor.userId());
        sendNotificationIfTarget(actor.userId(), ticket.getAssignedTo(),
                "ticket.cancelled",
                "Ticket cancelled",
                "Ticket '" + ticket.getTitle() + "' was cancelled",
                payload,
                "ticket:" + ticket.getId() + ":cancelled:user:" + ticket.getAssignedTo());
    }

    private void notifyComment(AuthPrincipal actor, Ticket ticket, TicketComment comment) {
        Set<String> targets = new HashSet<>();
        if (!actor.userId().equals(ticket.getCreatedBy())) {
            targets.add(ticket.getCreatedBy());
        }
        if (ticket.getAssignedTo() != null && !actor.userId().equals(ticket.getAssignedTo())) {
            targets.add(ticket.getAssignedTo());
        }
        if (targets.isEmpty()) {
            return;
        }
        Map<String, Object> payload = baseTicketPayload(ticket, actor.userId());
        payload.put("commentId", comment.getId());
        payload.put("comment", comment.getContent());
        for (String target : targets) {
            sendNotificationIfTarget(actor.userId(), target,
                    "ticket.commented",
                    "New ticket comment",
                    "New comment on ticket '" + ticket.getTitle() + "'",
                    payload,
                    "ticket:" + ticket.getId() + ":comment:" + comment.getId() + ":user:" + target);
        }
    }

    private void sendNotificationIfTarget(String actorId,
                                          String targetUserId,
                                          String topic,
                                          String title,
                                          String message,
                                          Map<String, Object> payload,
                                          String dedupeKey) {
        if (targetUserId == null || targetUserId.isBlank() || targetUserId.equals(actorId)) {
            return;
        }
        NotificationEvent event = new NotificationEvent(
                targetUserId,
                topic,
                title,
                message,
                ResourceType.TICKET,
                ticketIdFromPayload(payload),
                payload,
                dedupeKey
        );
        notificationPublisher.publish(event);
    }

    private String ticketIdFromPayload(Map<String, Object> payload) {
        Object id = payload != null ? payload.get("ticketId") : null;
        return id != null ? id.toString() : null;
    }

    private Map<String, Object> baseTicketPayload(Ticket ticket, String actorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("status", ticket.getStatus());
        payload.put("assignedTo", ticket.getAssignedTo());
        payload.put("actorId", actorId);
        payload.put("title", ticket.getTitle());
        return payload;
    }
}
