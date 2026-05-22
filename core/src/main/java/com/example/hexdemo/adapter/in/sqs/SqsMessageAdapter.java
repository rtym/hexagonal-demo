package com.example.hexdemo.adapter.in.sqs;

import com.example.hexdemo.application.port.in.ProcessMessageUseCase;
import com.example.hexdemo.domain.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Driving adapter — polls an SQS queue and forwards each message body to the use case.
 * Messages are deleted from the queue after successful processing.
 */
@Component
public class SqsMessageAdapter {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageAdapter.class);

    private final SqsClient sqsClient;
    private final ProcessMessageUseCase processMessageUseCase;
    private final String queueUrl;

    public SqsMessageAdapter(SqsClient sqsClient,
                              ProcessMessageUseCase processMessageUseCase,
                              @Value("${adapter.sqs.queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.processMessageUseCase = processMessageUseCase;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelayString = "${adapter.sqs.polling-interval-ms:5000}")
    public void poll() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(1)
                .build();

        List<software.amazon.awssdk.services.sqs.model.Message> sqsMessages;
        try {
            sqsMessages = sqsClient.receiveMessage(request).messages();
        } catch (Exception e) {
            log.warn("SQS poll failed (is LocalStack running?): {}", e.getMessage());
            return;
        }

        for (var sqsMsg : sqsMessages) {
            String body = sqsMsg.body().trim();
            log.info("SQS adapter: received '{}'", body);

            processMessageUseCase.processMessage(
                    new Message(UUID.randomUUID().toString(), body,
                            Message.MessageSource.SQS, Instant.now()));

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(sqsMsg.receiptHandle())
                    .build());
        }
    }
}
