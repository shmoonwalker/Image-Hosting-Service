package com.example.imagehostingservice.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class ObjectStorageConfig {

    @Bean
    S3Client s3Client(ObjectStorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(properties.getEndpoint())
                .region(software.amazon.awssdk.regions.Region.of(
                        properties.getRegion()
                ))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        properties.getAccessKeyId(),
                                        properties.getSecretAccessKey()
                                )
                        )
                )
                .forcePathStyle(true)
                .build();
    }
}