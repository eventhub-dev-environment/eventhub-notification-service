package com.eventhub.notification_service.infrastructure.adapter.in.web.dto;

import com.eventhub.notification_service.domain.model.NotificationStatus;
import com.eventhub.notification_service.domain.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        String id,
        String userId,
        String recipientEmail,
        String subject,
        String message,
        NotificationType type,
        NotificationStatus status,
        LocalDateTime createdAt
) {}