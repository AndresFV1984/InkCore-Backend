package com.inkcore.application.user.usecase;

import com.inkcore.application.shared.PasswordHasherPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.user.exception.UserAlreadyExistsException;
import com.inkcore.domain.user.model.Role;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.RoleRepositoryPort;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import com.inkcore.infrastructure.config.PasswordPolicyProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CreateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordHasherPort passwordHasher;
    private final PasswordPolicyProperties passwordPolicy;
    private final Clock clock;

    public CreateUserUseCase(
            UserRepositoryPort userRepository,
            RoleRepositoryPort roleRepository,
            PasswordHasherPort passwordHasher,
            PasswordPolicyProperties passwordPolicy,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    /**
     * Alta vía {@code POST /api/v1/users}.
     * Resuelve el rol por {@code roleName}, crea la relación en {@code user_roles}
     * y asigna los permisos asociados en BD.
     * No recibe campos de seguridad (tokenVersion, lock, etc.): los fija el servidor.
     */
    @Transactional
    public User create(
            String companyId,
            String identificationNumber,
            String documentType,
            String name,
            String mail,
            String contact,
            String department,
            String city,
            String address,
            String password,
            String roleName,
            Boolean state
    ) {
        if (userRepository.existsByMailIgnoreCase(mail)) {
            throw new UserAlreadyExistsException("mail", mail);
        }
        if (userRepository.existsByIdentificationNumber(identificationNumber)) {
            throw new UserAlreadyExistsException("identificationNumber", identificationNumber);
        }

        Role role = roleRepository.findByRoleName(roleName == null ? "" : roleName.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ROLE_NOT_FOUND",
                        "No existe el rol indicado: " + roleName
                ));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate creationDate = LocalDate.now(clock);
        boolean effectiveState = Objects.requireNonNullElse(state, true);

        User user = User.createForApi(
                companyId,
                identificationNumber,
                documentType,
                name,
                mail,
                contact,
                department,
                city,
                address,
                passwordHasher.hash(password),
                effectiveState,
                List.of(role.getRoleId()),
                List.of(role.getName()),
                List.of(role.getCode()),
                role.getPermissionCodes(),
                creationDate,
                now,
                now.plusDays(passwordPolicy.getExpirationDays())
        );

        return userRepository.save(user);
    }

    /** Alta vía formulario completo {@code POST /api/v1/users/register}. */
    @Transactional
    public User execute(CreateUserCommand command) {
        if (userRepository.existsByMailIgnoreCase(command.mail())) {
            throw new UserAlreadyExistsException("mail", command.mail());
        }
        if (userRepository.existsByIdentificationNumber(command.identificationNumber())) {
            throw new UserAlreadyExistsException("identificationNumber", command.identificationNumber());
        }

        String roleKey = command.roleCode() == null ? "" : command.roleCode().trim();
        Role role = roleRepository.findByCodeOrName(roleKey)
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

        LocalDateTime now = LocalDateTime.now(clock);
        User base = User.createNew(
                command.companyId(),
                command.identificationNumber(),
                command.documentType(),
                command.name(),
                command.mail(),
                command.contact(),
                command.department(),
                command.city(),
                command.address(),
                passwordHasher.hash(command.rawPassword()),
                command.state(),
                List.of(role.getRoleId()),
                List.of(role.getName()),
                List.of(role.getCode()),
                permissions
        );

        User withPasswordDates = User.reconstitute(
                base.getUserId(),
                base.getCompanyId(),
                base.getIdentificationNumber(),
                base.getDocumentType(),
                base.getName(),
                base.getMail(),
                base.getContact(),
                base.getDepartment(),
                base.getCity(),
                base.getAddress(),
                base.getPasswordHash(),
                base.getCreationDate(),
                base.isState(),
                base.getTokenVersion(),
                base.getRoleIds(),
                base.getRoleNames(),
                base.getRoleCodes(),
                base.getPermissionCodes(),
                true,
                now,
                now.plusDays(passwordPolicy.getExpirationDays()),
                0,
                null,
                null
        );

        return userRepository.save(withPasswordDates);
    }
}
