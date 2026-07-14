package com.indicore.application.user.usecase;

import com.indicore.application.shared.PasswordHasherPort;
import com.indicore.domain.shared.exception.ResourceNotFoundException;
import com.indicore.domain.user.exception.UserAlreadyExistsException;
import com.indicore.domain.user.model.Permission;
import com.indicore.domain.user.model.Role;
import com.indicore.domain.user.model.User;
import com.indicore.domain.user.ports.out.PermissionRepositoryPort;
import com.indicore.domain.user.ports.out.RoleRepositoryPort;
import com.indicore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UpdateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PermissionRepositoryPort permissionRepository;
    private final PasswordHasherPort passwordHasher;

    public UpdateUserUseCase(
            UserRepositoryPort userRepository,
            RoleRepositoryPort roleRepository,
            PermissionRepositoryPort permissionRepository,
            PasswordHasherPort passwordHasher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
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

        Role role = roleRepository.findByCodeIgnoreCase(command.roleCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ROLE_NOT_FOUND",
                        "No existe el rol indicado: " + command.roleCode()
                ));

        List<String> permissionCodes = resolvePermissionCodes(command.permissionCodes());

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
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                permissionCodes
        );

        return userRepository.save(updated);
    }

    private List<String> resolvePermissionCodes(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        List<String> normalized = requested.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();

        List<Permission> found = permissionRepository.findByCodes(normalized);
        Set<String> foundCodes = found.stream()
                .map(p -> p.getCode().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> missing = new HashSet<>(normalized);
        missing.removeAll(foundCodes);
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException(
                    "PERMISSION_NOT_FOUND",
                    "Permisos inexistentes: " + String.join(", ", missing)
            );
        }
        return found.stream().map(Permission::getCode).toList();
    }
}
