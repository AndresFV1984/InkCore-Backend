package com.inkcore.infrastructure.security;

import com.inkcore.application.shared.RefreshTokenRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRefreshTokenStoreTest {

    private static final Instant NOW = Instant.parse("2026-07-18T12:00:00Z");

    private InMemoryRefreshTokenStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRefreshTokenStore(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void saveAndFind_roundTrip() {
        RefreshTokenRecord record = new RefreshTokenRecord(
                "opaque-token",
                "user-1",
                2L,
                NOW.plusSeconds(3600)
        );
        store.save(record, Duration.ofSeconds(3600));

        assertEquals(record, store.find("opaque-token").orElseThrow());
    }

    @Test
    void find_returnsEmptyWhenExpired() {
        RefreshTokenRecord expired = new RefreshTokenRecord(
                "old",
                "user-1",
                1L,
                NOW.minusSeconds(1)
        );
        store.save(expired, Duration.ofSeconds(1));
        assertTrue(store.find("old").isEmpty());
    }

    @Test
    void deleteAllByUser_removesOnlyThatUser() {
        store.save(new RefreshTokenRecord("a", "u1", 1L, NOW.plusSeconds(60)), Duration.ofSeconds(60));
        store.save(new RefreshTokenRecord("b", "u2", 1L, NOW.plusSeconds(60)), Duration.ofSeconds(60));
        store.deleteAllByUser("u1");
        assertTrue(store.find("a").isEmpty());
        assertTrue(store.find("b").isPresent());
    }
}
