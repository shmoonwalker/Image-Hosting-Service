package com.example.imagehostingservice.image.tagging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageTaggingRabbitConfig {

    public static final String IMAGE_TAGGING_EXCHANGE =
            "image.tagging.exchange";

    public static final String IMAGE_TAGGING_QUEUE =
            "image.tagging.queue";

    public static final String IMAGE_TAGGING_ROUTING_KEY =
            "image.tagging.request";

    public static final String IMAGE_TAGGING_FAILED_EXCHANGE =
            "image.tagging.failed.exchange";

    public static final String IMAGE_TAGGING_FAILED_QUEUE =
            "image.tagging.failed.queue";

    public static final String IMAGE_TAGGING_FAILED_ROUTING_KEY =
            "image.tagging.failed";

    @Bean
    public DirectExchange imageTaggingExchange() {
        return new DirectExchange(
                IMAGE_TAGGING_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue imageTaggingQueue() {
        return QueueBuilder
                .durable(IMAGE_TAGGING_QUEUE)
                .deadLetterExchange(
                        IMAGE_TAGGING_FAILED_EXCHANGE
                )
                .deadLetterRoutingKey(
                        IMAGE_TAGGING_FAILED_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Binding imageTaggingBinding(
            Queue imageTaggingQueue,
            DirectExchange imageTaggingExchange
    ) {
        return BindingBuilder
                .bind(imageTaggingQueue)
                .to(imageTaggingExchange)
                .with(IMAGE_TAGGING_ROUTING_KEY);
    }

    @Bean
    public DirectExchange imageTaggingFailedExchange() {
        return new DirectExchange(
                IMAGE_TAGGING_FAILED_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue imageTaggingFailedQueue() {
        return QueueBuilder
                .durable(IMAGE_TAGGING_FAILED_QUEUE)
                .build();
    }

    @Bean
    public Binding imageTaggingFailedBinding(
            Queue imageTaggingFailedQueue,
            DirectExchange imageTaggingFailedExchange
    ) {
        return BindingBuilder
                .bind(imageTaggingFailedQueue)
                .to(imageTaggingFailedExchange)
                .with(IMAGE_TAGGING_FAILED_ROUTING_KEY);
    }
}