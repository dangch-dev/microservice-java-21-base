package pl.co.notification.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;
import pl.co.notification.dto.NotificationPageResponse;
import pl.co.notification.dto.NotificationResponse;
import pl.co.notification.service.NotificationService;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationPageResponse> list(Authentication authentication,
                                                      @RequestParam(defaultValue = "0") @Min(0) Integer page,
                                                      @RequestParam(defaultValue = "20") @Min(1) Integer size) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(notificationService.list(userId, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(notificationService.unreadCount(userId));
    }

    @PostMapping("/{id}/seen")
    public ApiResponse<NotificationResponse> markSeen(Authentication authentication,
                                                      @PathVariable("id") String id) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(notificationService.markSeen(userId, id));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(Authentication authentication,
                                                      @PathVariable("id") String id) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(notificationService.markRead(userId, id));
    }

    @PostMapping("/seen/all")
    public ApiResponse<Void> markAllSeen(Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        notificationService.markAllSeen(userId);
        return ApiResponse.ok(null);
    }
}
