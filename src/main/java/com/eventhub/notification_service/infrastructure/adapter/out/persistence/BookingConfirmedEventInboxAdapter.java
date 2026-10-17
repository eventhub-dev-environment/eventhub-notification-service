package com.eventhub.notification_service.infrastructure.adapter.out.persistence;

import com.eventhub.notification_service.domain.port.out.BookingConfirmedEventInboxPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BookingConfirmedEventInboxAdapter implements BookingConfirmedEventInboxPort {

    private final MongoBookingConfirmedEventInboxRepository repository;

    @Override
    public boolean isProcessed(String messageId) {
        return repository.existsById(messageId);
    }

    @Override
    public void markProcessed(String messageId) {
        repository.save(BookingConfirmedEventInboxDocument.builder()
                .messageId(messageId)
                .processedAt(LocalDateTime.now())
                .build());
    }
}
