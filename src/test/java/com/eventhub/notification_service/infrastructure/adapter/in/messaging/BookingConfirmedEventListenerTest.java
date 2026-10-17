package com.eventhub.notification_service.infrastructure.adapter.in.messaging;

import com.eventhub.notification_service.domain.port.out.NotificationRepositoryPort;
import com.eventhub.notification_service.domain.port.out.BookingConfirmedEventInboxPort;
import com.eventhub.notification_service.infrastructure.adapter.out.email.EmailServiceAdapter;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.EventServiceClient;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class BookingConfirmedEventListenerTest {

    @Test
    void rejectsConfirmedBookingEventWithoutRecipientEmail() {
        EmailServiceAdapter emailService = mock(EmailServiceAdapter.class);
        EventServiceClient eventServiceClient = mock(EventServiceClient.class);
        NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
        BookingConfirmedEventInboxPort inbox = mock(BookingConfirmedEventInboxPort.class);
        BookingConfirmedEventListener listener = new BookingConfirmedEventListener(
                emailService,
                eventServiceClient,
                notificationRepository,
                inbox);

        assertThatThrownBy(() -> listener.handleBookingConfirmed(Map.of(
                "bookingId", "booking-123",
                "eventId", "event-123",
                "userId", "user-123",
                "transactionId", "transaction-123",
                "totalPrice", "42.50"), "message-123"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(emailService, eventServiceClient, notificationRepository);
        verifyNoInteractions(inbox);
    }

    @Test
    void deduplicatesLegacyConfirmedBookingWithoutMessageIdByBookingId() {
        EmailServiceAdapter emailService = mock(EmailServiceAdapter.class);
        EventServiceClient eventServiceClient = mock(EventServiceClient.class);
        NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
        BookingConfirmedEventInboxPort inbox = mock(BookingConfirmedEventInboxPort.class);
        when(inbox.isProcessed("legacy-booking-confirmed:booking-123")).thenReturn(true);
        BookingConfirmedEventListener listener = new BookingConfirmedEventListener(
                emailService,
                eventServiceClient,
                notificationRepository,
                inbox);

        listener.handleBookingConfirmed(validEvent(), null);

        verify(inbox).isProcessed("legacy-booking-confirmed:booking-123");
        verifyNoInteractions(emailService, eventServiceClient, notificationRepository);
    }

    @Test
    void skipsAlreadyProcessedConfirmedBookingEvent() {
        EmailServiceAdapter emailService = mock(EmailServiceAdapter.class);
        EventServiceClient eventServiceClient = mock(EventServiceClient.class);
        NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
        BookingConfirmedEventInboxPort inbox = mock(BookingConfirmedEventInboxPort.class);
        when(inbox.isProcessed("message-123")).thenReturn(true);
        BookingConfirmedEventListener listener = new BookingConfirmedEventListener(
                emailService,
                eventServiceClient,
                notificationRepository,
                inbox);

        listener.handleBookingConfirmed(validEvent(), "message-123");

        verify(inbox).isProcessed("message-123");
        verifyNoInteractions(emailService, eventServiceClient, notificationRepository);
    }

    @Test
    void marksEventProcessedAfterSendingAndPersistingNotification() {
        EmailServiceAdapter emailService = mock(EmailServiceAdapter.class);
        EventServiceClient eventServiceClient = mock(EventServiceClient.class);
        NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
        BookingConfirmedEventInboxPort inbox = mock(BookingConfirmedEventInboxPort.class);
        BookingConfirmedEventListener listener = new BookingConfirmedEventListener(
                emailService,
                eventServiceClient,
                notificationRepository,
                inbox);

        listener.handleBookingConfirmed(validEvent(), "message-123");

        InOrder inOrder = inOrder(emailService, notificationRepository, inbox);
        inOrder.verify(emailService).sendTicketConfirmationEmail(
                ArgumentMatchers.eq("buyer@example.com"),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq("booking-123"),
                ArgumentMatchers.eq("transaction-123"),
                ArgumentMatchers.any(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(byte[].class));
        inOrder.verify(notificationRepository).save(ArgumentMatchers.any());
        inOrder.verify(inbox).markProcessed("message-123");
    }

    @Test
    void leavesEventUnprocessedWhenEmailSendingFails() {
        EmailServiceAdapter emailService = mock(EmailServiceAdapter.class);
        EventServiceClient eventServiceClient = mock(EventServiceClient.class);
        NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
        BookingConfirmedEventInboxPort inbox = mock(BookingConfirmedEventInboxPort.class);
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService)
                .sendTicketConfirmationEmail(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.anyList(),
                        ArgumentMatchers.any(byte[].class));
        BookingConfirmedEventListener listener = new BookingConfirmedEventListener(
                emailService,
                eventServiceClient,
                notificationRepository,
                inbox);

        assertThatThrownBy(() -> listener.handleBookingConfirmed(validEvent(), "message-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SMTP unavailable");

        verify(inbox).isProcessed("message-123");
        verify(notificationRepository, never()).save(ArgumentMatchers.any());
        verify(inbox, never()).markProcessed("message-123");
    }

    private Map<String, Object> validEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("bookingId", "booking-123");
        event.put("eventId", "event-123");
        event.put("userId", "user-123");
        event.put("userEmail", "buyer@example.com");
        event.put("transactionId", "transaction-123");
        event.put("totalPrice", "42.50");
        return event;
    }
}
