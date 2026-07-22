package com.inkcore.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Activa Redis solo cuando {@code security.refresh-token.store.type=redis}.
 * Con el default {@code memory}, Redis no se conecta ni se exige en arranque.
 */
@Configuration
@ConditionalOnProperty(name = "security.refresh-token.store.type", havingValue = "redis")
@Import(RedisAutoConfiguration.class)
public class RedisStoreConfig {
}
