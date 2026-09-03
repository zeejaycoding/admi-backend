package com.powercity.power_city_platform.controller;

import com.powercity.power_city_platform.dto.response.common.ApiResponse;
import com.powercity.power_city_platform.dto.response.notification.NotificationResponse;
import com.powercity.power_city_platform.entity.User;
import com.powercity.power_city_platform.service.NotificationService;
import com.powercity.power_city_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "APIs for user notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get my notifications", description = "Get the current user's notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications() {
        User currentUser = userService.getCurrentUser();
        List<NotificationResponse> notifications = notificationService.getForUser(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count", description = "Get the current user's unread notification count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        User currentUser = userService.getCurrentUser();
        long count = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully",
                Map.of("unread", count)));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Mark all of the current user's notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        User currentUser = userService.getCurrentUser();
        notificationService.markAllRead(currentUser);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a single notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        notificationService.markRead(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }
}
