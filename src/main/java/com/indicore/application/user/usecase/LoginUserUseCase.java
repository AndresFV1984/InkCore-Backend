package com.indicore.application.user.usecase;

import com.indicore.application.shared.AccessTokenPort;
import com.indicore.application.shared.PasswordHasherPort;
import com.indicore.domain.user.exception.AccountDisabledException;
import com.indicore.domain.user.exception.AccountLockedException;
import com.indicore.domain.user.exception.InvalidCredentialsException;
import com.indicore.domain.user.model.User;
import com.indicore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;

    public LoginUserUseCase(
            UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            AccessTokenPort accessTokenPort
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
    }

    @Transactional
    public LoginResult execute(LoginUserCommand command) {
        User user = userRepository.findByMailIgnoreCase(command.mail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isState()) {
            throw new AccountDisabledException();
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new AccountLockedException();
        }

        if (!passwordHasher.matches(command.rawPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        userRepository.updateLastLoginAt(user.getUserId(), now);

        List<String> roles = user.getRoleCode() == null || user.getRoleCode().isBlank()
                ? List.of()
                : List.of(user.getRoleCode());

        String token = accessTokenPort.createAccessToken(
                user.getUserId(),
                user.getTokenVersion(),
                roles
        );

        return new LoginResult(token, "Bearer", accessTokenPort.getExpirationSeconds());
    }
}
