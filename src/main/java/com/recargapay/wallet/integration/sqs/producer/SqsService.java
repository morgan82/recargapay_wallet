package com.recargapay.wallet.integration.sqs.producer;

import com.recargapay.wallet.helper.JsonHelper;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqsService {
    private final SqsTemplate sqsTemplate;
    private final JsonHelper jsonHelper;

    public <T> void doEnqueue(String queueName, Message<T> message) {
        SendResult<T> send = sqsTemplate.send(queueName, message);
        log.debug("Message:{}, enqueued to:{}", jsonHelper.serialize(send.message().getPayload()), send.endpoint());
    }
}
