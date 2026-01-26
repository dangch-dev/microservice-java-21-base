package pl.co.identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;
import pl.co.identity.dto.TicketCommentRequest;
import pl.co.identity.dto.TicketCommentResponse;
import pl.co.identity.dto.TicketFilterRequest;
import pl.co.identity.dto.TicketPageResponse;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.dto.TicketStatusUpdateRequest;
import pl.co.identity.service.TicketManagementService;

/**
 * Management endpoints for operators/admins handling any ticket.
 */
@RestController
@RequestMapping("/management/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_ADMIN.name())")
public class TicketManagementController {

    private final TicketManagementService ticketManagementService;

    @GetMapping
    public ApiResponse<TicketPageResponse> list(Authentication authentication,
                                                @Valid TicketFilterRequest filter) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketManagementService.list(userId, filter));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketResponse> get(Authentication authentication,
                                           @PathVariable String ticketId) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketManagementService.get(userId, ticketId));
    }

    @PatchMapping("/{ticketId}/status")
    public ApiResponse<TicketResponse> updateStatus(Authentication authentication,
                                                    @PathVariable String ticketId,
                                                    @Valid @RequestBody TicketStatusUpdateRequest request) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketManagementService.updateStatus(userId, ticketId, request));
    }

    @PostMapping("/{ticketId}/comments")
    public ApiResponse<TicketCommentResponse> addComment(Authentication authentication,
                                                         @PathVariable String ticketId,
                                                         @Valid @RequestBody TicketCommentRequest request) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketManagementService.addComment(userId, ticketId, request));
    }

    @GetMapping("/{ticketId}/comments")
    public ApiResponse<java.util.List<TicketCommentResponse>> listComments(Authentication authentication,
                                                                           @PathVariable String ticketId) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(ticketManagementService.listComments(userId, ticketId));
    }
}
