package com.inkcore.domain.user.ports.out;

import com.inkcore.domain.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(String userId);

    Optional<User> findByMailIgnoreCase(String mail);

    boolean existsByMailIgnoreCase(String mail);

    boolean existsByMailIgnoreCaseExcludingUserId(String mail, String userId);

    boolean existsByIdentificationNumber(String identificationNumber);

    boolean existsByIdentificationNumberExcludingUserId(String identificationNumber, String userId);

    List<User> findAll();

    List<User> findAllByState(boolean state);

    /** Versión de token actual para validar claim {@code tv} del JWT */
    Optional<Long> findTokenVersionByUserId(String userId);

    void updateLastLoginAt(String userId, java.time.LocalDateTime at);
}
