package com.inkcore.infrastructure.config;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.application.inkestimateasset.usecase.InkEstimateAssetSupport;
import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import com.inkcore.infrastructure.out.objectstorage.InMemoryObjectStorageAdapter;
import com.inkcore.infrastructure.out.objectstorage.S3ObjectStorageAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

@Configuration
@EnableScheduling
public class ObjectStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "inkcore.object-storage.type", havingValue = "s3", matchIfMissing = true)
    ObjectStoragePort s3ObjectStoragePort(ObjectStorageProperties properties) {
        return new S3ObjectStorageAdapter(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "inkcore.object-storage.type", havingValue = "memory")
    ObjectStoragePort inMemoryObjectStoragePort() {
        return new InMemoryObjectStorageAdapter();
    }

    @Bean
    InkEstimateAssetFileValidator.ObjectStoragePropertiesReader inkEstimateAssetSizeLimits(
            ObjectStorageProperties properties
    ) {
        return new InkEstimateAssetFileValidator.ObjectStoragePropertiesReader(
                properties.getMaxAssetFileBytes(),
                properties.getMaxPreviewFileBytes()
        );
    }

    @Bean
    InkEstimateAssetSupport.InkEstimateAssetStorageSettings inkEstimateAssetStorageSettings(
            ObjectStorageProperties properties
    ) {
        int ttl = Math.max(60, Math.min(900, properties.getPresignedUrlTtlSeconds()));
        return new InkEstimateAssetSupport.InkEstimateAssetStorageSettings(Duration.ofSeconds(ttl));
    }
}
