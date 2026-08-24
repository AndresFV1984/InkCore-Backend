package com.inkcore.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectStorageSecretsValidatorTest {

    @Test
    void rejectsDevSecretWhenInsecureDefaultsDisallowed() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setType("s3");
        properties.setSecretKey(ObjectStorageProperties.INSECURE_DEFAULT_SECRET);
        properties.setAllowInsecureDefaults(false);
        ObjectStorageSecretsValidator validator = new ObjectStorageSecretsValidator(properties);
        assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    void rejectsMinioRootSecretWhenInsecureDefaultsDisallowed() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setSecretKey("minioadmin");
        properties.setAllowInsecureDefaults(false);
        ObjectStorageSecretsValidator validator = new ObjectStorageSecretsValidator(properties);
        assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    void allowsDevSecretWhenFlagEnabled() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setSecretKey(ObjectStorageProperties.INSECURE_DEFAULT_SECRET);
        properties.setAllowInsecureDefaults(true);
        ObjectStorageSecretsValidator validator = new ObjectStorageSecretsValidator(properties);
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    void allowsCustomSecret() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setSecretKey("prod-rotated-secret");
        properties.setAllowInsecureDefaults(false);
        ObjectStorageSecretsValidator validator = new ObjectStorageSecretsValidator(properties);
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }
}
