package com.inkcore.application.shared;

import java.time.Duration;
import java.util.Optional;

/**
 * Persistencia de refresh tokens opacos (memoria o Redis).
 */
public interface RefreshTokenStorePort {

    void save(RefreshTokenRecord record, Duration ttl);

    Optional<RefreshTokenRecord> find(String refreshToken);

    void delete(String refreshToken);

    void deleteAllByUser(String userId);
}
