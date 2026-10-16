package com.eventhub.notification_service.domain.port.out;

import com.eventhub.notification_service.domain.model.Notification;
import java.util.List;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    List<Notification> findByUserId(String userId);
}