package com.eventhub.notification_service.infrastructure.adapter.in.messaging;

import com.eventhub.notification_service.domain.util.PdfTicketGenerator;
import com.eventhub.notification_service.domain.util.QRCodeGenerator;
import com.eventhub.notification_service.infrastructure.adapter.out.email.EmailServiceAdapter;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.EventServiceClient;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.ApiResponse;
import com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.EventResponseDto;
import com.eventhub.notification_service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmedEventListener {

    private final EmailServiceAdapter emailServiceAdapter;
    private final EventServiceClient eventServiceClient;

    @Value("${gateway.internal-secret:internal-secret-eventhub-2026}")
    private String gatewaySecret;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_BOOKING_CONFIRMED_QUEUE)
    public void handleBookingConfirmed(Map<String, Object> event) {
        log.info(">>>> EVENTO BOOKING_CONFIRMED RECIBIDO EN NOTIFICATION-SERVICE: {}", event);

        String bookingId = (String) event.get("bookingId");
        String eventId = (String) event.get("eventId");
        String userId = (String) event.get("userId");
        List<Map<String, Object>> rawTickets = (List<Map<String, Object>>) event.get("tickets");

        // 1. Obtener Branding desde event-service vía Feign
        EventResponseDto eventData = null;
        try {
            ApiResponse<EventResponseDto> response = eventServiceClient.getEventById(eventId, gatewaySecret);
            if (response != null && response.isSuccess()) {
                eventData = response.getData();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener la información de branding para el evento {}: {}", eventId, e.getMessage());
        }

        // 2. Procesar tickets y generar imágenes QR en bytes
        // 2. Procesar tickets y generar imágenes QR en bytes
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

        String userName = "Juan Pablo";
        String userEmail = "runner.juank2026@gmail.com";

        // 3. Generar el PDF
        byte[] pdfBytes = PdfTicketGenerator.generateTicketPdf(userName, bookingId, eventData, processedTickets);

        // 4. Despachar correo con PDF adjunto
        emailServiceAdapter.sendTicketConfirmationEmail(
                userEmail,
                userName,
                bookingId,
                "TXN-" + System.currentTimeMillis(),
                BigDecimal.valueOf(85000),
                eventData,
                processedTickets,
                pdfBytes
        );
    }
}