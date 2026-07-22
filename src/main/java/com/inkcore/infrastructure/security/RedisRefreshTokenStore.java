package com.inkcore.infrastructure.security;

import com.inkcore.application.shared.RefreshTokenRecord;
import com.inkcore.application.shared.RefreshTokenStorePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Store de refresh tokens en Redis.
 * Clave: {@code refresh:{token}} → value {@code userId|tokenVersion|expiresAtEpochMs}
 * Índice: {@code refresh:user:{userId}} → set de tokens
 */
@Component
@ConditionalOnProperty(name = "security.refresh-token.store.type", havingValue = "redis")
public class RedisRefreshTokenStore implements RefreshTokenStorePort {

    private static final String KEY_PREFIX = "refresh:";
    private static final String USER_INDEX_PREFIX = "refresh:user:";
    private static final String SEP = "|";

    private final StringRedisTemplate redis;

    public RedisRefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(RefreshTokenRecord record, Duration ttl) {
        String key = KEY_PREFIX + record.refreshToken();
        String value = record.userId() + SEP + record.tokenVersion() + SEP + record.expiresAt().toEpochMilli();
        redis.opsForValue().set(key, value, ttl);
        redis.opsForSet().add(USER_INDEX_PREFIX + record.userId(), record.refreshToken());
        redis.expire(USER_INDEX_PREFIX + record.userId(), ttl);
    }

    @Override
    public Optional<RefreshTokenRecord> find(String refreshToken) {
        String raw = redis.opsForValue().get(KEY_PREFIX + refreshToken);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.split("\\|", 3);
        if (parts.length != 3) {
            delete(refreshToken);
            return Optional.empty();
        }
        try {
            String userId = parts[0];
            long tokenVersion = Long.parseLong(parts[1]);
            Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(parts[2]));
            if (expiresAt.isBefore(Instant.now())) {
                delete(refreshToken);
                return Optional.empty();
            }
            return Optional.of(new RefreshTokenRecord(refreshToken, userId, tokenVersion, expiresAt));
        } catch (NumberFormatException ex) {
            delete(refreshToken);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String refreshToken) {
        String key = KEY_PREFIX + refreshToken;
        String raw = redis.opsForValue().get(key);
        redis.delete(key);
        if (raw != null) {
            String userId = raw.split("\\|", 2)[0];
            redis.opsForSet().remove(USER_INDEX_PREFIX + userId, refreshToken);
        }
    }

    @Override
    public void deleteAllByUser(String userId) {
        String indexKey = USER_INDEX_PREFIX + userId;
        Set<String> tokens = redis.opsForSet().members(indexKey);
        if (tokens != null) {
            for (String token : tokens) {
                redis.delete(KEY_PREFIX + token);
            }
        }
        redis.delete(indexKey);
    }
}
