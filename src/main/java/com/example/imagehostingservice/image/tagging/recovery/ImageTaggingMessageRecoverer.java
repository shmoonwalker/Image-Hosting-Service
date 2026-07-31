package com.example.imagehostingservice.image.tagging.recovery;

import com.example.imagehostingservice.image.tagging.repository.ImageTaggingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageTaggingMessageRecoverer
        implements MessageRecoverer {

    private final ImageTaggingRepository taggingRepository;

    private final RejectAndDontRequeueRecoverer rejectRecoverer =
            new RejectAndDontRequeueRecoverer(
                    "Image tagging retries exhausted"
            );

    @Override
    public void recover(
            Message message,
            Throwable cause
    ) {
        String payload = new String(
                message.getBody(),
                StandardCharsets.UTF_8
        );

        try {
            Long imageId = Long.valueOf(payload);

            boolean markedFailed =
                    taggingRepository.markFailed(imageId);

            log.error(
                    "Image tagging retries exhausted "
                            + "imageId={} markedFailed={}",
                    imageId,
                    markedFailed,
                    cause
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Could not mark exhausted tagging message as failed",
                    exception
            );
        }

        rejectRecoverer.recover(message, cause);
    }
}