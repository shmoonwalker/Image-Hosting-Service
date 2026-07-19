package com.example.imagehostingservice.storage.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.net.URI;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "object-storage")
public class ObjectStorageProperties {

    @NotNull
    private URI endpoint;

    @NotBlank
    private String region;

    @NotBlank
    private String accessKeyId;

    @NotBlank
    private String secretAccessKey;

    @NotBlank
    private String bucket;
}
