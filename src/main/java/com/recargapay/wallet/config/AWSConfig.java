package com.recargapay.wallet.config;

import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;
import java.util.concurrent.Executors;

@Configuration
public class AWSConfig {
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("admin", "admin"));
    }

    @Bean
    public ClientAsyncConfiguration clientAsyncConfiguration() {
        val virtualThreadFactory = Thread.ofVirtual().name("client-aws-async", 0).factory();
        return ClientAsyncConfiguration
                .builder()
                .advancedOption(
                        SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                        Executors.newThreadPerTaskExecutor(virtualThreadFactory))
                .build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient(AwsCredentialsProvider provider,
                                         ClientAsyncConfiguration asyncConfiguration,
                                         SdkAsyncHttpClient sdkAsyncHttpClient) {
        return SqsAsyncClient
                .builder()
                .asyncConfiguration(asyncConfiguration)
                .httpClient(sdkAsyncHttpClient)
                .credentialsProvider(provider)
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.EU_WEST_1)
                .build();
    }

    @Bean
    public SdkAsyncHttpClient sdkAsyncHttpClient() {
        return NettyNioAsyncHttpClient
                .builder()
                .build();
    }
}
