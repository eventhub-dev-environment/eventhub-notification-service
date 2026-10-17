package com.eventhub.notification_service.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "booking_confirmed_inbox")
public class BookingConfirmedEventInboxDocument {
    @Id
    private String messageId;
    private LocalDateTime processedAt;
}
