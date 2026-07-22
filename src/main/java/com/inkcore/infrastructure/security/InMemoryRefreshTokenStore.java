package com.inkcore.infrastructure.security;

import com.inkcore.application.shared.RefreshTokenRecord;
import com.inkcore.application.shared.RefreshTokenStorePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "security.refresh-token.store.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryRefreshTokenStore implements RefreshTokenStorePort {

    private final ConcurrentHashMap<String, RefreshTokenRecord> byToken = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRefreshTokenStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(RefreshTokenRecord record, Duration ttl) {
        Instant expiresAt = record.expiresAt() != null
                ? record.expiresAt()
                : Instant.now(clock).plus(ttl);
        byToken.put(record.refreshToken(), new RefreshTokenRecord(
                record.refreshToken(),
                record.userId(),
                record.tokenVersion(),
                expiresAt
        ));
    }

    @Override
    public Optional<RefreshTokenRecord> find(String refreshToken) {
        RefreshTokenRecord record = byToken.get(refreshToken);
        if (record == null) {
            return Optional.empty();
        }
        if (record.expiresAt().isBefore(Instant.now(clock))) {
            byToken.remove(refreshToken, record);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void delete(String refreshToken) {
        byToken.remove(refreshToken);
    }

    @Override
    public void deleteAllByUser(String userId) {
        byToken.entrySet().removeIf(e -> userId.equals(e.getValue().userId()));
    }
}
