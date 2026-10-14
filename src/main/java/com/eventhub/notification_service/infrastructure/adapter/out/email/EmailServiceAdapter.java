package com.eventhub.notification_service.infrastructure.adapter.out.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceAdapter {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendTicketConfirmationEmail(
            String to,
            String userName,
            String bookingId,
            String transactionId,
            Object amount,
            Object eventData,
            List<Map<String, Object>> tickets,
            byte[] pdfBytes
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("transactionId", transactionId);
            context.setVariable("amount", amount);
            context.setVariable("event", eventData);
            context.setVariable("branding", eventData != null ? ((com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.EventResponseDto) eventData).getBranding() : null);
            context.setVariable("tickets", tickets);

            String htmlContent = templateEngine.process("mail/ticket-confirmation", context);

            helper.setTo(to);
            helper.setSubject("Tus Entradas - " + (eventData != null ? ((com.eventhub.notification_service.infrastructure.adapter.out.feign.dto.EventResponseDto) eventData).getTitle() : "EventHub"));
            helper.setText(htmlContent, true);

            // Adjuntar el PDF oficial del ticket
            if (pdfBytes != null) {
                helper.addAttachment("Entradas-EventHub-" + bookingId + ".pdf", new ByteArrayResource(pdfBytes));
            }

            mailSender.send(message);
            log.info("Correo de confirmación enviado exitosamente con PDF adjunto a {}", to);
        } catch (Exception e) {
            log.error("Error al enviar el correo de confirmación: {}", e.getMessage(), e);
        }
    }
}