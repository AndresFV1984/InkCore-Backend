package com.indicore.domain.user.ports.out;

import com.indicore.domain.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(String userId);

    Optional<User> findByMailIgnoreCase(String mail);

    boolean existsByMailIgnoreCase(String mail);

    boolean existsByIdentificationNumber(String identificationNumber);

    List<User> findAll();

    /** Versión de token actual para validar claim {@code tv} del JWT */
    Optional<Long> findTokenVersionByUserId(String userId);

    void updateLastLoginAt(String userId, java.time.LocalDateTime at);
}
