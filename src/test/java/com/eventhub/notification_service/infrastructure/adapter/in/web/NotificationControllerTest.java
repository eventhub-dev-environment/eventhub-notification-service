package com.eventhub.notification_service.infrastructure.adapter.in.web;

import com.eventhub.notification_service.domain.port.out.NotificationRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    @Test
    void loadsOnlyNotificationsForAuthenticatedUser() {
        NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
        when(repository.findByUserId("user-123")).thenReturn(List.of());

        new NotificationController(repository).getMyNotifications("user-123");

        verify(repository).findByUserId("user-123");
    }
}
