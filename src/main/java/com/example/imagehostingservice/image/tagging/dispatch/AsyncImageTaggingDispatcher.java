package com.example.imagehostingservice.image.tagging.dispatch;

import com.example.imagehostingservice.image.tagging.service.ImageTaggingProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsyncImageTaggingDispatcher
        implements ImageTaggingDispatcher {

    private final ImageTaggingProcessor processor;

    @Async
    @Override
    public void dispatch(Long imageId) {
        processor.process(imageId);
    }
}