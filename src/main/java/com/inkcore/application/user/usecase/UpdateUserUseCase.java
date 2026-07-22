package com.inkcore.application.user.usecase;

import com.inkcore.application.shared.PasswordHasherPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.user.exception.UserAlreadyExistsException;
import com.inkcore.domain.user.model.Role;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.RoleRepositoryPort;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UpdateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordHasherPort passwordHasher;

    public UpdateUserUseCase(
            UserRepositoryPort userRepository,
            RoleRepositoryPort roleRepository,
            PasswordHasherPort passwordHasher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public User execute(UpdateUserCommand command) {
        User existing = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND",
                        "Usuario no encontrado"
                ));

        if (userRepository.existsByMailIgnoreCaseExcludingUserId(command.mail(), command.userId())) {
            throw new UserAlreadyExistsException("Ya existe un usuario con el correo indicado");
        }
        if (userRepository.existsByIdentificationNumberExcludingUserId(
                command.identificationNumber(),
                command.userId()
        )) {
            throw new UserAlreadyExistsException("Ya existe un usuario con el documento indicado");
        }

        Role role = roleRepository.findByCodeOrName(command.roleCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ROLE_NOT_FOUND",
                        "No existe el rol indicado: " + command.roleCode()
                ));

        List<String> permissions = role.getPermissionCodes();
        if (command.permissionCodes() != null && !command.permissionCodes().isEmpty()) {
            permissions = command.permissionCodes().stream()
                    .filter(c -> c != null && !c.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }

        String hashedPassword = null;
        if (command.rawPassword() != null && !command.rawPassword().isBlank()) {
            if (command.rawPassword().length() < 6) {
                throw new IllegalArgumentException("La contraseña debe tener mínimo 6 caracteres");
            }
            hashedPassword = passwordHasher.hash(command.rawPassword());
        }

        User updated = existing.update(
                command.identificationNumber(),
                command.documentType(),
                command.name(),
                command.mail(),
                command.contact(),
                command.department(),
                command.city(),
                command.address(),
                hashedPassword,
                command.state(),
                List.of(role.getRoleId()),
                List.of(role.getName()),
                List.of(role.getCode()),
                permissions
        );

        return userRepository.save(updated);
    }
}
