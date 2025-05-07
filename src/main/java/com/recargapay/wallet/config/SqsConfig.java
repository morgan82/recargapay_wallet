package com.recargapay.wallet.config;

import io.awspring.cloud.sqs.MessageExecutionThreadFactory;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ListenerMode;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsConfig {
    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient,
            @Qualifier("messageConverterForAWS") MappingJackson2MessageConverter jackson2MessageConverter) {
        val messageConverter = new SqsMessagingMessageConverter();
        messageConverter.setPayloadMessageConverter(jackson2MessageConverter);
        return SqsMessageListenerContainerFactory
                .builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> {
                    options.messageConverter(messageConverter);
                    options.componentsTaskExecutor(createTaskExecutor());
                    options.maxMessagesPerPoll(10);//default is 10
                    options.maxConcurrentMessages(10);//default is 10
                    options.acknowledgementMode(AcknowledgementMode.MANUAL);
                    options.listenerMode(ListenerMode.SINGLE_MESSAGE);
                })
                .build();
    }

    protected TaskExecutor createTaskExecutor() {
        val executor = new SimpleAsyncTaskExecutor();
        executor.setThreadFactory(new MessageExecutionThreadFactory("sqs-listener-"));
        return executor;
    }
}
