package com.example.hexdemo.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

/**
 * Wires AWS SDK v2 clients.
 * When {@code aws.endpoint-override} is set (LocalStack local dev), both clients
 * point to that endpoint with dummy credentials — no real AWS account needed.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.endpoint-override:}")
    private String endpointOverride;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .region(Region.of(region));

        if (!endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
                   .credentialsProvider(localCredentials());
        }
        return builder.build();
    }

    @Bean
    public SqsClient sqsClient() {
        var builder = SqsClient.builder()
                .region(Region.of(region));

        if (!endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
                   .credentialsProvider(localCredentials());
        }
        return builder.build();
    }

    private StaticCredentialsProvider localCredentials() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create("localstack", "localstack"));
    }
}
