package com.eventhub.notification_service.infrastructure.adapter.out.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailServiceAdapterTest {

    @Test
    void propagatesSmtpFailuresToTheMessageListener() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailSendException smtpFailure = new MailSendException("SMTP unavailable");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");
        doThrow(smtpFailure).when(mailSender).send(mimeMessage);
        EmailServiceAdapter adapter = new EmailServiceAdapter(mailSender, templateEngine);

        assertThatThrownBy(() -> adapter.sendTicketConfirmationEmail(
                "buyer@example.com",
                "Test User",
                "booking-123",
                "transaction-123",
                42.50,
                null,
                List.<Map<String, Object>>of(),
                null))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseReference(smtpFailure);
    }
}
