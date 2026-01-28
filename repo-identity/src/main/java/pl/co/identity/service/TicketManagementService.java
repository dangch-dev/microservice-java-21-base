package pl.co.identity.service;

import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;

import java.util.List;

public interface TicketManagementService {
    TicketPageResponse list(String userId, TicketFilterRequest filter);
    TicketResponse get(String userId, String ticketId);
    TicketResponse updateStatus(String userId, String ticketId, TicketStatusUpdateRequest request);
    TicketCommentResponse addComment(String userId, String ticketId, TicketCommentRequest request);
    List<TicketCommentResponse> listComments(String userId, String ticketId);
}
