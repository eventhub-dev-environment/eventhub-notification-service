package com.eventhub.notification_service.infrastructure.adapter.in.web;

import com.eventhub.notification_service.domain.model.Notification;
import com.eventhub.notification_service.domain.port.out.NotificationRepositoryPort;
import com.eventhub.notification_service.infrastructure.adapter.in.web.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepositoryPort notificationRepositoryPort;

    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestHeader("X-User-Id") String userId
    ) {
        List<NotificationResponse> response = notificationRepositoryPort.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getRecipientEmail(),
                notification.getSubject(),
                notification.getMessage(),
                notification.getType(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}