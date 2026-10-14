package com.eventhub.notification_service.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter; // Importación correcta
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String NOTIFICATION_BOOKING_CONFIRMED_QUEUE = "notification.booking.confirmed.queue";
    public static final String BOOKING_CONFIRMED_KEY = "booking.confirmed";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationBookingConfirmedQueue() {
        return QueueBuilder.durable(NOTIFICATION_BOOKING_CONFIRMED_QUEUE).build();
    }

    @Bean
    public Binding notificationBookingConfirmedBinding(Queue notificationBookingConfirmedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationBookingConfirmedQueue)
                .to(bookingExchange)
                .with(BOOKING_CONFIRMED_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(); // <--- Jackson2JsonMessageConverter
    }
}