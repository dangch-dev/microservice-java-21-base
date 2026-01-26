package pl.co.identity.service;

import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.identity.dto.TicketCreateRequest;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;

import java.util.List;

public interface TicketUserService {
    TicketResponse create(String userId, TicketCreateRequest request);
    TicketPageResponse list(String userId, TicketFilterRequest filter);
    TicketResponse get(String userId, String ticketId);
    TicketResponse cancel(String userId, String ticketId);
    TicketCommentResponse addComment(String userId, String ticketId, TicketCommentRequest request);
    List<TicketCommentResponse> listComments(String userId, String ticketId);
}
