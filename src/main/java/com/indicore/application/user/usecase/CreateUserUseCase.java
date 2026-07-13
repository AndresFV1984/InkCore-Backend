package com.indicore.application.user.usecase;

import com.indicore.domain.user.exception.UserAlreadyExistsException;
import com.indicore.domain.user.model.User;
import com.indicore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserUseCase(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User execute(CreateUserCommand command) {
        if (userRepository.existsByMailIgnoreCase(command.mail())) {
            throw new UserAlreadyExistsException("Ya existe un usuario con el correo indicado");
        }
        if (userRepository.existsByIdentificationNumber(command.identificationNumber())) {
            throw new UserAlreadyExistsException("Ya existe un usuario con el documento indicado");
        }

        User user = User.createNew(
                command.companyId(),
                command.identificationNumber(),
                command.documentType(),
                command.name(),
                command.mail(),
                command.contact(),
                command.address(),
                passwordEncoder.encode(command.rawPassword()),
                command.roleId(),
                command.roleCode()
        );

        return userRepository.save(user);
    }
}
