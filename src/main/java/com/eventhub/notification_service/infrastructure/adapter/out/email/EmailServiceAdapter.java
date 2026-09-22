package com.eventhub.notification_service.infrastructure.adapter.out.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceAdapter {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendTicketConfirmationEmail(String toEmail, String userName, String bookingId,
                                            String transactionId, BigDecimal amount,
                                            List<Map<String, String>> tickets) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("transactionId", transactionId);
            context.setVariable("amount", amount);
            context.setVariable("tickets", tickets);

            String htmlContent = templateEngine.process("mail/ticket-confirmation", context);

            helper.setTo(toEmail);
            helper.setSubject(" Confirmación de tu Reserva #" + bookingId + " - EventHub");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Correo de confirmación enviado exitosamente a {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error al construir/enviar el correo de confirmación a {}: {}", toEmail, e.getMessage());
        }
    }
}