package com.inkcore.infrastructure.out.cache;

import com.inkcore.domain.colorconversion.ports.out.IccProfileCachePort;
import com.inkcore.infrastructure.config.ColorConversionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché ICC en memoria (default). Activo cuando
 * {@code inkcore.color-conversion.icc-cache.type=memory} o la propiedad no está definida.
 */
@Component
@ConditionalOnProperty(
        prefix = "inkcore.color-conversion.icc-cache",
        name = "type",
        havingValue = "memory",
        matchIfMissing = true
)
public class InMemoryIccProfileCacheAdapter implements IccProfileCachePort {

    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public InMemoryIccProfileCacheAdapter(ColorConversionProperties properties) {
        this.ttlSeconds = Math.max(1, properties.getIccCache().getTtlSeconds());
    }

    @Override
    public Optional<byte[]> get(String profileKey) {
        CacheEntry entry = store.get(profileKey);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            store.remove(profileKey);
            return Optional.empty();
        }
        return Optional.of(entry.bytes());
    }

    @Override
    public void put(String profileKey, byte[] profileBytes) {
        store.put(profileKey, new CacheEntry(profileBytes, Instant.now().plusSeconds(ttlSeconds)));
    }

    private record CacheEntry(byte[] bytes, Instant expiresAt) {
    }
}
