package com.inkcore.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Activa Redis cuando cualquier store/cache lo pide.
 * Antes solo miraba refresh-token: si ICC_CACHE_TYPE=redis y refresh=memory
 * no había {@code StringRedisTemplate} y el arranque fallaba (equipos nuevos con .env).
 */
@Configuration
@ConditionalOnExpression(
        "'${security.refresh-token.store.type:memory}'.equalsIgnoreCase('redis')"
                + " or '${security.token-version-cache.type:memory}'.equalsIgnoreCase('redis')"
                + " or '${inkcore.color-conversion.icc-cache.type:memory}'.equalsIgnoreCase('redis')"
)
@Import(RedisAutoConfiguration.class)
public class RedisStoreConfig {
}
