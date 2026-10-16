package com.eventhub.notification_service.infrastructure.adapter.out.persistence;

import com.eventhub.notification_service.domain.model.Notification;
import com.eventhub.notification_service.domain.port.out.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final MongoNotificationRepository mongoRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationDocument doc = NotificationDocument.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .recipientEmail(notification.getRecipientEmail())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .type(notification.getType())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .build();

        NotificationDocument saved = mongoRepository.save(doc);
        notification.setId(saved.getId());
        return notification;
    }

    @Override
    public List<Notification> findByUserId(String userId) {
        return mongoRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Notification toDomain(NotificationDocument doc) {
        return Notification.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .recipientEmail(doc.getRecipientEmail())
                .subject(doc.getSubject())
                .message(doc.getMessage())
                .type(doc.getType())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}