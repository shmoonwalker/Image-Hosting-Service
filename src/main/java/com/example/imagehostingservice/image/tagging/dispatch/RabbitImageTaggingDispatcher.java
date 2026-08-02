package com.example.imagehostingservice.image.tagging.dispatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.imagehostingservice.image.tagging.config.ImageTaggingRabbitConfig.IMAGE_TAGGING_EXCHANGE;
import static com.example.imagehostingservice.image.tagging.config.ImageTaggingRabbitConfig.IMAGE_TAGGING_ROUTING_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitImageTaggingDispatcher
        implements ImageTaggingDispatcher {

    private static final long PUBLISH_CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void dispatch(Long imageId) {
        CorrelationData correlationData =
                new CorrelationData(imageId.toString());

        rabbitTemplate.convertAndSend(
                IMAGE_TAGGING_EXCHANGE,
                IMAGE_TAGGING_ROUTING_KEY,
                imageId.toString(),
                correlationData
        );

        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(
                            PUBLISH_CONFIRM_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            if (!confirm.ack()) {
                throw new IllegalStateException(
                        "RabbitMQ rejected image tagging message: "
                                + confirm.reason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ could not route image tagging message"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting for RabbitMQ confirmation",
                    exception
            );
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    "RabbitMQ did not confirm image tagging message",
                    exception
            );
        }

        log.info(
                "Image tagging message confirmed imageId={}",
                imageId
        );
    }
}
