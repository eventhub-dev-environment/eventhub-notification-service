package com.eventhub.notification_service.infrastructure.adapter.in.messaging;

import com.eventhub.notification_service.domain.model.Notification;
import com.eventhub.notification_service.domain.model.NotificationStatus;
import com.eventhub.notification_service.domain.model.NotificationType;
import com.eventhub.notification_service.domain.port.out.BookingConfirmedEventInboxPort;
import com.eventhub.notification_service.domain.port.out.NotificationRepositoryPort;
import com.eventhub.notification_service.domain.util.PdfTicketGenerator;
import com.eventhub.notification_service.domain.util.QRCodeGenerator;
import com.eventhub.notification_service.infrastructure.adapter.out.email.EmailServiceAdapter;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.EventServiceClient;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.ApiResponse;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.EventResponseDto;
import com.eventhub.notification_service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmedEventListener {

    private final EmailServiceAdapter emailServiceAdapter;
    private final EventServiceClient eventServiceClient;
    private final NotificationRepositoryPort notificationRepositoryPort;
    private final BookingConfirmedEventInboxPort bookingConfirmedEventInboxPort;

    @Value("${gateway.internal-secret}")
    private String gatewaySecret;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_BOOKING_CONFIRMED_QUEUE)
    public void handleBookingConfirmed(
            Map<String, Object> event,
            @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        String bookingId = (String) event.get("bookingId");
        String eventId = (String) event.get("eventId");
        String userId = (String) event.get("userId");
        String userEmail = (String) event.get("userEmail");
        String transactionId = (String) event.get("transactionId");
        Object totalPrice = event.get("totalPrice");

        if (isBlank(bookingId) || isBlank(eventId) || isBlank(userId)
                || isBlank(userEmail) || isBlank(transactionId) || totalPrice == null) {
            log.error("Evento de reserva confirmado inválido; falta información requerida para enviar la notificación");
            throw new AmqpRejectAndDontRequeueException("Evento de reserva confirmado incompleto");
        }

        String inboxId = isBlank(messageId) ? "legacy-booking-confirmed:" + bookingId : messageId;
        if (bookingConfirmedEventInboxPort.isProcessed(inboxId)) {
            log.info("Evento de reserva confirmado ya procesado; se omite el mensaje {}", inboxId);
            return;
        }

        log.info("Evento de reserva confirmada recibido para bookingId: {}", bookingId);

        // 1. Extraer datos dinámicos del payload (sin valores quemados)
        String userName = event.get("userName") != null ? (String) event.get("userName") : "Usuario EventHub";
        BigDecimal amount = new BigDecimal(totalPrice.toString());

        List<Map<String, Object>> rawTickets = (List<Map<String, Object>>) event.get("tickets");

        // 2. Obtener Branding desde event-service vía Feign
        EventResponseDto eventData = null;
        try {
            ApiResponse<EventResponseDto> response = eventServiceClient.getEventById(eventId, gatewaySecret);
            if (response != null && response.isSuccess()) {
                eventData = response.getData();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener la información de branding para el evento {}: {}", eventId, e.getMessage());
        }

        // 3. Procesar tickets y generar imágenes QR dinámicas
        List<Map<String, Object>> processedTickets = new ArrayList<>();
        if (rawTickets != null) {
            for (Map<String, Object> t : rawTickets) {
                String ticketId = (String) t.get("ticketId");
                String qrHash = (String) t.get("qrCodeHash");
                String tierId = (String) t.get("ticketTierId");

                String qrPayload = "https://api.eventhub.com/api/v1/bookings/tickets/validate?ticketId=" + ticketId + "&qrCodeHash=" + qrHash;
                byte[] qrBytes = QRCodeGenerator.generateQRCodeBytes(qrPayload, 200, 200);

                processedTickets.add(Map.of(
                        "ticketId", ticketId,
                        "tierName", tierId != null ? tierId : "Entrada General",
                        "qrBytes", qrBytes
                ));
            }
        }

        // 4. Generar el PDF oficial del ticket
        byte[] pdfBytes = PdfTicketGenerator.generateTicketPdf(userName, bookingId, eventData, processedTickets);

        // 5. Despachar correo con PDF adjunto
        String eventTitle = eventData != null ? eventData.getTitle() : "Evento EventHub";
        String subject = "Tus Entradas - " + eventTitle;

        emailServiceAdapter.sendTicketConfirmationEmail(
                userEmail,
                userName,
                bookingId,
                transactionId,
                amount,
                eventData,
                processedTickets,
                pdfBytes
        );

        // 6. Persistir el registro de notificación en MongoDB
        notificationRepositoryPort.save(Notification.builder()
                .userId(userId)
                .recipientEmail(userEmail)
                .subject(subject)
                .message("Confirmación y tickets enviados para la reserva " + bookingId)
                .type(NotificationType.BOOKING_CONFIRMED)
                .status(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build());

        bookingConfirmedEventInboxPort.markProcessed(inboxId);
        log.info(">>>> NOTIFICACIÓN GUARDADA EN MONGODB PARA EL USUARIO {}", userId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}