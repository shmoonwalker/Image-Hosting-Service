package com.example.imagehostingservice.image.tagging.dispatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.imagehostingservice.image.tagging.config.ImageTaggingRabbitConfig.IMAGE_TAGGING_EXCHANGE;
import static com.example.imagehostingservice.image.tagging.config.ImageTaggingRabbitConfig.IMAGE_TAGGING_ROUTING_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitImageTaggingDispatcher
        implements ImageTaggingDispatcher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void dispatch(Long imageId) {
        rabbitTemplate.convertAndSend(
                IMAGE_TAGGING_EXCHANGE,
                IMAGE_TAGGING_ROUTING_KEY,
                imageId.toString()
        );

        log.info(
                "Image tagging message published imageId={}",
                imageId
        );
    }
}