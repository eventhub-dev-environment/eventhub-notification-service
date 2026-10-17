package com.eventhub.notification_service.domain.port.out;

public interface BookingConfirmedEventInboxPort {
    boolean isProcessed(String messageId);
    void markProcessed(String messageId);
}
