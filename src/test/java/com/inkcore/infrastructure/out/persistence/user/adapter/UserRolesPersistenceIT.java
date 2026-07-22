package com.inkcore.infrastructure.out.persistence.user.adapter;

import com.inkcore.application.user.usecase.CreateUserUseCase;
import com.inkcore.domain.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica contra Postgres real (profile dev) que el alta inserta {@code user_roles}.
 * Requiere DB local accesible con las credenciales de application-dev.yaml.
 */
@SpringBootTest
@ActiveProfiles("dev")
class UserRolesPersistenceIT {

    @Autowired
    CreateUserUseCase createUserUseCase;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createUser_insertsUserRolesRow() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String mail = "role-it-" + suffix + "@example.com";
        String identification = "IT" + suffix;

        User created = createUserUseCase.create(
                "company-seed-001",
                identification,
                "CC",
                "IT User " + suffix,
                mail,
                "3000000000",
                "Antioquia",
                "Medellín",
                "Calle IT",
                "SecurePass123*",
                "Administrador",
                true
        );

        assertFalse(created.getRoleIds().isEmpty(), "dominio debe traer roleIds");
        assertFalse(created.getRoleCodes().isEmpty(), "dominio debe traer roleCodes");
        assertFalse(created.getPermissionCodes().isEmpty(), "dominio debe traer permisos del rol");

        Integer links = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM indicolors.user_roles WHERE user_id = ?",
                Integer.class,
                created.getUserId()
        );
        assertEquals(1, links, "debe existir exactamente 1 fila en user_roles");

        String roleName = jdbcTemplate.queryForObject(
                """
                        SELECT r.name
                        FROM indicolors.user_roles ur
                        JOIN indicolors.roles r ON r.role_id = ur.role_id
                        WHERE ur.user_id = ?
                        """,
                String.class,
                created.getUserId()
        );
        assertEquals("Administrador", roleName);

        // cleanup
        jdbcTemplate.update("DELETE FROM indicolors.user_roles WHERE user_id = ?", created.getUserId());
        jdbcTemplate.update("DELETE FROM indicolors.users WHERE user_id = ?", created.getUserId());
        assertTrue(true);
    }
}
