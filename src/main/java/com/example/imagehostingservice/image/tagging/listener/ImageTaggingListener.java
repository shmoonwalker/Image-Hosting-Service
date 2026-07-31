package com.example.imagehostingservice.image.tagging.listener;

import com.example.imagehostingservice.image.tagging.service.ImageTaggingProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.imagehostingservice.image.tagging.config.ImageTaggingRabbitConfig.IMAGE_TAGGING_QUEUE;

@Component
@RequiredArgsConstructor
public class ImageTaggingListener {

    private final ImageTaggingProcessor imageTaggingProcessor;

    @RabbitListener(queues = IMAGE_TAGGING_QUEUE)
    public void handleImageTagging(String imageId) {
        imageTaggingProcessor.process(
                Long.valueOf(imageId)
        );
    }
}