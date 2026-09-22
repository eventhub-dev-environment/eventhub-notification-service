package com.eventhub.notification_service.infrastructure.adapter.in.messaging;

import com.eventhub.notification_service.infrastructure.adapter.out.email.EmailServiceAdapter;
import com.eventhub.notification_service.infrastructure.config.RabbitMQConfig;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventListener {

    private final EmailServiceAdapter emailServiceAdapter;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unused")
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_PAYMENT_QUEUE)
    public void handlePaymentCompleted(Map<String, Object> event) {
        log.info(">>>> EVENTO RECIBIDO EN NOTIFICATION-SERVICE: {}", event);

        String bookingId = (String) event.get("bookingId");
        String transactionId = (String) event.get("transactionId");
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        String rawPayloadStr = (String) event.get("rawPayload");

        String userEmail = "runner.juank2026@eventhub.com"; // Email por defecto
        try {
            if (rawPayloadStr != null && !rawPayloadStr.isBlank()) {
                Map<String, Object> rawPayload = objectMapper.readValue(rawPayloadStr, Map.class);
                if (rawPayload.containsKey("user_email")) {
                    userEmail = (String) rawPayload.get("user_email");
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer user_email de rawPayload, usando email por defecto: {}", e.getMessage());
        }

        // Mock de tickets asignados a la reserva para renderizar en la plantilla HTML
        List<Map<String, String>> tickets = List.of(
                Map.of("tierName", "10K Neón VIP", "qrCodeHash", "qr-hash-a1b2c3d4-2026"),
                Map.of("tierName", "10K Neón VIP", "qrCodeHash", "qr-hash-e5f6g7h8-2026")
        );

        log.info("Despachando correo de confirmación a: {}", userEmail);

        emailServiceAdapter.sendTicketConfirmationEmail(
                userEmail,
                "Juan Pablo",
                bookingId,
                transactionId,
                amount,
                tickets
        );
    }
}