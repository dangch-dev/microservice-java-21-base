package pl.co.identity.service;

import pl.co.identity.dto.TicketCreateRequest;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;
import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.common.security.AuthPrincipal;

import java.util.List;

public interface TicketService {
    TicketResponse create(AuthPrincipal principal, TicketCreateRequest request);
    TicketPageResponse list(AuthPrincipal principal, TicketFilterRequest filter);
    TicketResponse get(AuthPrincipal principal, String ticketId);
    TicketResponse updateStatus(AuthPrincipal principal, String ticketId, TicketStatusUpdateRequest request);
    TicketResponse cancel(AuthPrincipal principal, String ticketId);
    TicketCommentResponse addComment(AuthPrincipal principal, String ticketId, TicketCommentRequest request);
    List<TicketCommentResponse> listComments(AuthPrincipal principal, String ticketId);
}
