package com.eventhub.notification_service.infrastructure.adapter.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoBookingConfirmedEventInboxRepository
        extends MongoRepository<BookingConfirmedEventInboxDocument, String> {
}
