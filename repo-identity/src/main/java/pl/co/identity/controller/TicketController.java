package pl.co.identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;
import pl.co.identity.dto.TicketCreateRequest;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;
import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.identity.service.TicketUserService;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketUserService ticketUserService;

    @PostMapping
    public ApiResponse<TicketResponse> create(Authentication authentication,
                                              @Valid @RequestBody TicketCreateRequest request) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketUserService.create(userId, request));
    }

    @GetMapping
    public ApiResponse<TicketPageResponse> list(Authentication authentication,
                                                @Valid TicketFilterRequest filter) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketUserService.list(userId, filter));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketResponse> get(Authentication authentication,
                                           @PathVariable String ticketId) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketUserService.get(userId, ticketId));
    }

    @PostMapping("/{ticketId}/cancel")
    public ApiResponse<TicketResponse> cancel(Authentication authentication,
                                              @PathVariable String ticketId) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketUserService.cancel(userId, ticketId));
    }

    @PostMapping("/{ticketId}/comments")
    public ApiResponse<TicketCommentResponse> addComment(Authentication authentication,
                                                         @PathVariable String ticketId,
                                                         @Valid @RequestBody TicketCommentRequest request) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketUserService.addComment(userId, ticketId, request));
    }

    @GetMapping("/{ticketId}/comments")
    public ApiResponse<java.util.List<TicketCommentResponse>> listComments(Authentication authentication,
                                                                           @PathVariable String ticketId) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketUserService.listComments(userId, ticketId));
    }
}
