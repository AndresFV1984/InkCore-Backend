package com.inkcore.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Impide arrancar en ambientes reales con el secreto de ejemplo de Compose.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "inkcore.object-storage.type", havingValue = "s3", matchIfMissing = true)
public class ObjectStorageSecretsValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageSecretsValidator.class);

    private final ObjectStorageProperties properties;

    public ObjectStorageSecretsValidator(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String secret = properties.getSecretKey() == null ? "" : properties.getSecretKey();
        boolean insecure = ObjectStorageProperties.INSECURE_DEFAULT_SECRET.equals(secret)
                || "minioadmin".equals(secret);
        if (!insecure) {
            return;
        }
        if (properties.isAllowInsecureDefaults()) {
            log.warn("Object storage usa un secreto de desarrollo (allow-insecure-defaults=true)");
            return;
        }
        throw new IllegalStateException(
                "OBJECT_STORAGE_SECRET_KEY es un valor de desarrollo. "
                        + "Defina un secreto real, o solo en local/docker ponga "
                        + "inkcore.object-storage.allow-insecure-defaults=true");
    }
}
