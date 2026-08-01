package com.inkcore.domain.colorconversion.ports.out;

import java.util.Optional;

/**
 * Caché de bytes de perfiles ICC ya cargados (TTL configurable).
 */
public interface IccProfileCachePort {

    Optional<byte[]> get(String profileKey);

    void put(String profileKey, byte[] profileBytes);
}
