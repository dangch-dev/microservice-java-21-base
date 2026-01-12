package pl.co.identity.service;

import pl.co.common.filter.principal.AuthPrincipal;
import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;

import java.util.List;

public interface TicketManagementService {
    TicketPageResponse list(AuthPrincipal principal, TicketFilterRequest filter);
    TicketResponse get(AuthPrincipal principal, String ticketId);
    TicketResponse updateStatus(AuthPrincipal principal, String ticketId, TicketStatusUpdateRequest request);
    TicketCommentResponse addComment(AuthPrincipal principal, String ticketId, TicketCommentRequest request);
    List<TicketCommentResponse> listComments(AuthPrincipal principal, String ticketId);
}
