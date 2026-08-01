package com.inkcore.infrastructure.out.cache;

import com.inkcore.domain.colorconversion.ports.out.IccProfileCachePort;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Caché Redis de bytes ICC (Base64). Activo cuando
 * {@code inkcore.color-conversion.icc-cache.type=redis} y existe {@link StringRedisTemplate}
 * (p. ej. {@code security.refresh-token.store.type=redis}).
 */
@Component
@ConditionalOnProperty(prefix = "inkcore.color-conversion.icc-cache", name = "type", havingValue = "redis")
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisIccProfileCacheAdapter implements IccProfileCachePort {

    private static final String KEY_PREFIX = "inkcore:icc-profile:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisIccProfileCacheAdapter(
            StringRedisTemplate redisTemplate,
            ColorConversionProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(Math.max(1, properties.getIccCache().getTtlSeconds()));
    }

    @Override
    public Optional<byte[]> get(String profileKey) {
        String encoded = redisTemplate.opsForValue().get(KEY_PREFIX + profileKey);
        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Base64.getDecoder().decode(encoded));
    }

    @Override
    public void put(String profileKey, byte[] profileBytes) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + profileKey,
                Base64.getEncoder().encodeToString(profileBytes),
                ttl
        );
    }
}
