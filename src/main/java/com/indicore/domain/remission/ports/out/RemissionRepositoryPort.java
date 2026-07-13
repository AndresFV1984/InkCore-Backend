package com.indicore.domain.remission.ports.out;

import com.indicore.domain.remission.model.Remission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RemissionRepositoryPort {

    Remission save(Remission remission);

    Optional<Remission> findById(UUID id);

    List<Remission> findAll();
}
