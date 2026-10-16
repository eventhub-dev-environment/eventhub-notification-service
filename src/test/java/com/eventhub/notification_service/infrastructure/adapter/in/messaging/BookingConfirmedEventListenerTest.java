package com.eventhub.notification_service.infrastructure.adapter.in.messaging;

import com.eventhub.notification_service.domain.port.out.NotificationRepositoryPort;
import com.eventhub.notification_service.infrastructure.adapter.out.email.EmailServiceAdapter;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.EventServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class BookingConfirmedEventListenerTest {

    @Test
    void rejectsConfirmedBookingEventWithoutRecipientEmail() {
        EmailServiceAdapter emailService = mock(EmailServiceAdapter.class);
        EventServiceClient eventServiceClient = mock(EventServiceClient.class);
        NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
        BookingConfirmedEventListener listener = new BookingConfirmedEventListener(
                emailService,
                eventServiceClient,
                notificationRepository);

        assertThatThrownBy(() -> listener.handleBookingConfirmed(Map.of(
                "bookingId", "booking-123",
                "eventId", "event-123",
                "userId", "user-123",
                "transactionId", "transaction-123",
                "totalPrice", "42.50")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(emailService, eventServiceClient, notificationRepository);
    }
}
