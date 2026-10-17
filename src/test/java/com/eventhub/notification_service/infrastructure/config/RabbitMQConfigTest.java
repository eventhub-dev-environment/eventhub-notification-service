package com.eventhub.notification_service.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    @Test
    void routesExhaustedBookingConfirmationMessagesToDeadLetterQueue() {
        Queue queue = new RabbitMQConfig().notificationBookingConfirmedQueue();

        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.NOTIFICATION_BOOKING_CONFIRMED_QUEUE);
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMQConfig.NOTIFICATION_DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitMQConfig.NOTIFICATION_BOOKING_CONFIRMED_DLQ_KEY);
    }
}
