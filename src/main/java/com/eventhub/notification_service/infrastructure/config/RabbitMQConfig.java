package com.eventhub.notification_service.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String NOTIFICATION_BOOKING_CONFIRMED_QUEUE = "notification.booking.confirmed.queue";
    public static final String NOTIFICATION_BOOKING_CONFIRMED_DLQ = "notification.booking.confirmed.dlq";
    public static final String NOTIFICATION_DEAD_LETTER_EXCHANGE = "notification.booking.dlx";
    public static final String BOOKING_CONFIRMED_KEY = "booking.confirmed";
    public static final String NOTIFICATION_BOOKING_CONFIRMED_DLQ_KEY = "notification.booking.confirmed.dead";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationBookingConfirmedQueue() {
        return QueueBuilder.durable(NOTIFICATION_BOOKING_CONFIRMED_QUEUE)
                .deadLetterExchange(NOTIFICATION_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(NOTIFICATION_BOOKING_CONFIRMED_DLQ_KEY)
                .build();
    }

    @Bean
    public DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue notificationBookingConfirmedDlq() {
        return QueueBuilder.durable(NOTIFICATION_BOOKING_CONFIRMED_DLQ).build();
    }

    @Bean
    public Binding notificationBookingConfirmedBinding(Queue notificationBookingConfirmedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationBookingConfirmedQueue)
                .to(bookingExchange)
                .with(BOOKING_CONFIRMED_KEY);
    }

    @Bean
    public Binding notificationBookingConfirmedDlqBinding(
            Queue notificationBookingConfirmedDlq,
            DirectExchange notificationDeadLetterExchange) {
        return BindingBuilder.bind(notificationBookingConfirmedDlq)
                .to(notificationDeadLetterExchange)
                .with(NOTIFICATION_BOOKING_CONFIRMED_DLQ_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000, 2.0, 10_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }
}
