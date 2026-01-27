package pl.co.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.file.FileMeta;
import pl.co.common.event.EventPublisher;
import pl.co.common.notification.NotificationAction;
import pl.co.common.notification.NotificationEvent;
import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;
import pl.co.identity.entity.Ticket;
import pl.co.identity.entity.TicketComment;
import pl.co.identity.entity.TicketStatus;
import pl.co.identity.mapper.TicketMapper;
import pl.co.identity.repository.TicketCommentRepository;
import pl.co.identity.repository.TicketRepository;
import pl.co.identity.repository.UserRepository;
import pl.co.identity.service.TicketManagementService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Management service: assumes caller has been authorized (ADMIN/TICKET_MANAGER).
 */
@Service
@RequiredArgsConstructor
public class TicketManagementServiceImpl implements TicketManagementService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Value("${kafka.topics.notification}")
    private String notificationTopic;

    @Value("${kafka.topics.file}")
    private String fileTopic;

    @Override
    @Transactional(readOnly = true)
    public TicketPageResponse list(String userId, TicketFilterRequest filter) {
        int pageValue = filter.getPage() == null ? 0 : filter.getPage();
        int sizeValue = filter.getSize() == null ? 20 : filter.getSize();
        PageRequest page = PageRequest.of(Math.max(pageValue, 0), Math.max(sizeValue, 1));
        Specification<Ticket> spec = buildSpec(filter);
        Page<Ticket> result = ticketRepository.findAll(spec, page);
        List<TicketResponse> items = result.getContent().stream().map(this::toManagementResponse).toList();
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
        return toManagementResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateStatus(String userId, String ticketId, TicketStatusUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        String previousStatus = ticket.getStatus();
        String previousAssignee = ticket.getAssignedTo();
        String newAssigneeName = null;

        if (request.getAssignedTo() != null && !request.getAssignedTo().isBlank()) {
            var user = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ApiException(ErrorCode.E227, "Assignee not found"));
            ticket.setAssignedTo(request.getAssignedTo());
            newAssigneeName = user.getFullName();
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            ticket.setStatus(validateTicketStatus(request.getStatus()));
        }

        Ticket saved = ticketRepository.save(ticket);
        notifyAssignmentAndStatus(userId, saved, previousStatus, previousAssignee, newAssigneeName);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketCommentResponse addComment(String userId, String ticketId, TicketCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
        TicketComment comment = TicketComment.builder()
                .ticketId(ticketId)
                .content(request.getContent())
                .files(request.getFiles())
                .build();
        TicketComment saved = ticketCommentRepository.save(comment);

        // Publish file
        publishFiles(request.getFiles());
        // Publish notification
        notifyComment(userId, ticket, saved);

        return toCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCommentResponse> listComments(String userId, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "Ticket not found"));
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

    private Specification<Ticket> buildSpec(TicketFilterRequest filter) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), validateTicketStatus(filter.getStatus())));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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

    private TicketResponse toManagementResponse(Ticket ticket) {
        var creator = ticket.getCreatedBy() == null ? null : userRepository.findById(ticket.getCreatedBy()).orElse(null);
        TicketStatus status = ticket.getStatus() == null ? null : TicketStatus.valueOf(ticket.getStatus());
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(status)
                .createdBy(ticket.getCreatedBy())
                .creatorName(creator == null ? null : creator.getFullName())
                .creatorAvatarUrl(creator == null ? null : creator.getAvatarUrl())
                .assignedTo(ticket.getAssignedTo())
                .files(ticket.getFiles())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private void notifyAssignmentAndStatus(String actorId, Ticket ticket, String previousStatus, String previousAssignee, String assigneeName) {
        if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().equals(previousAssignee)) {
            String assigneeId = ticket.getAssignedTo();
            String creatorId = ticket.getCreatedBy();
            Map<String, Object> payload = baseTicketPayload(ticket, actorId);
            payload.put("assigneeName", assigneeName);
            if (!assigneeId.isBlank() && !assigneeId.equals(actorId)) {
                eventPublisher.publish(notificationTopic, assigneeId, new NotificationEvent(
                        assigneeId,
                        NotificationAction.TICKET_ASSIGNED.name(),
                        NotificationAction.TICKET_ASSIGNED.title(),
                        NotificationAction.TICKET_ASSIGNED.message(ticket.getTitle(), "you"),
                        ticket.getId(),
                        payload,
                        "ticket:" + ticket.getId() + ":assigned:" + assigneeId + ":user:" + assigneeId
                ));
            }
            if (!creatorId.equals(assigneeId)) {
                if (!creatorId.isBlank() && !creatorId.equals(actorId)) {
                    eventPublisher.publish(notificationTopic, creatorId, new NotificationEvent(
                            creatorId,
                            NotificationAction.TICKET_ASSIGNED.name(),
                            NotificationAction.TICKET_ASSIGNED.title(),
                            NotificationAction.TICKET_ASSIGNED.message(
                                    ticket.getTitle(),
                                    assigneeName != null ? assigneeName : assigneeId),
                            ticket.getId(),
                            payload,
                            "ticket:" + ticket.getId() + ":assigned:" + assigneeId + ":user:" + creatorId
                    ));
                }
            }
        }
        if (!java.util.Objects.equals(previousStatus, ticket.getStatus())) {
            notifyStatusChange(actorId, ticket, previousStatus);
        }
    }

    private void notifyStatusChange(String actorId, Ticket ticket, String previousStatus) {
        Map<String, Object> payload = baseTicketPayload(ticket, actorId);
        payload.put("previousStatus", previousStatus);
        Set<String> targets = new HashSet<>();
        targets.add(ticket.getCreatedBy());
        if (ticket.getAssignedTo() != null) {
            targets.add(ticket.getAssignedTo());
        }
        targets.remove(actorId);
        for (String target : targets) {
            if (target == null || target.isBlank() || target.equals(actorId)) {
                continue;
            }
            eventPublisher.publish(notificationTopic, target, new NotificationEvent(
                    target,
                    NotificationAction.TICKET_STATUS_UPDATED.name(),
                    NotificationAction.TICKET_STATUS_UPDATED.title(),
                    NotificationAction.TICKET_STATUS_UPDATED.message(ticket.getTitle(), ticket.getStatus()),
                    ticket.getId(),
                    payload,
                    "ticket:" + ticket.getId() + ":status:" + ticket.getStatus() + ":user:" + target
            ));
        }
    }

    private void notifyComment(String actorId, Ticket ticket, TicketComment comment) {
        Set<String> targets = new HashSet<>();
        if (!actorId.equals(ticket.getCreatedBy())) {
            targets.add(ticket.getCreatedBy());
        }
        if (ticket.getAssignedTo() != null && !actorId.equals(ticket.getAssignedTo())) {
            targets.add(ticket.getAssignedTo());
        }
        targets.remove(actorId);
        if (targets.isEmpty()) {
            return;
        }
        Map<String, Object> payload = baseTicketPayload(ticket, actorId);
        payload.put("commentId", comment.getId());
        payload.put("comment", comment.getContent());
        for (String target : targets) {
            if (target == null || target.isBlank() || target.equals(actorId)) {
                continue;
            }
            eventPublisher.publish(notificationTopic, target, new NotificationEvent(
                    target,
                    NotificationAction.TICKET_COMMENT_ADDED.name(),
                    NotificationAction.TICKET_COMMENT_ADDED.title(),
                    NotificationAction.TICKET_COMMENT_ADDED.message(ticket.getTitle()),
                    ticket.getId(),
                    payload,
                    "ticket:" + ticket.getId() + ":comment:" + comment.getId() + ":user:" + target
            ));
        }
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

    private void publishFiles(List<FileMeta> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        List<String> fileIds = files.stream()
                .map(FileMeta::fileId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (fileIds.isEmpty()) {
            return;
        }
        eventPublisher.publish(fileTopic, null, fileIds);
    }
}
