-- =============================================================================
-- INFORMACIÓN — Consulta / ajuste de permisos a roles (NO es migración Flyway)
-- =============================================================================
-- Ubicación: scripts/postgres/assign-role-permissions.sql
-- Semilla inicial: V5__role_permissions.sql (DDL + INSERT).
-- Usa este script solo para consultar o ajustar permisos a mano (psql/DBeaver).
--
-- Tablas involucradas:
--   indicolors.permissions       → catálogo (semilla en V4__permissions.sql)
--   indicolors.roles             → roles (semilla en V3__roles.sql)
--   indicolors.role_permissions  → N:M (DDL + semilla en V5__role_permissions.sql)
--
-- Roles semilla:
--   Administrador → a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
--   Operador      → b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) Consultar catálogo de permisos
-- -----------------------------------------------------------------------------
-- SELECT permission_id, code, name, module FROM indicolors.permissions ORDER BY module, code;

-- -----------------------------------------------------------------------------
-- 2) Consultar permisos actuales de un rol (por nombre)
-- -----------------------------------------------------------------------------
-- SELECT r.name AS role, p.code, p.name AS permission
-- FROM indicolors.role_permissions rp
-- JOIN indicolors.roles r ON r.role_id = rp.role_id
-- JOIN indicolors.permissions p ON p.permission_id = rp.permission_id
-- WHERE r.name = 'Administrador'
-- ORDER BY p.code;

-- -----------------------------------------------------------------------------
-- 3) Asignar TODOS los permisos al rol Administrador
-- -----------------------------------------------------------------------------
INSERT INTO indicolors.role_permissions (role_id, permission_id)
SELECT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, p.permission_id
FROM indicolors.permissions p
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- 4) Asignar permisos operativos al rol Operador
-- -----------------------------------------------------------------------------
INSERT INTO indicolors.role_permissions (role_id, permission_id)
SELECT 'b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid, p.permission_id
FROM indicolors.permissions p
WHERE p.code IN (
    'production.orders.create_edit',
    'production.orders.view',
    'production.status.view',
    'production.stages.mark',
    'production.preprensa.mark',
    'production.corte.mark',
    'production.impresion.mark',
    'production.terminados.mark',
    'production.acabados.mark',
    'production.mywork.manage'
)
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- 5) Asignar un permiso puntual a un rol (ejemplo)
-- -----------------------------------------------------------------------------
-- INSERT INTO indicolors.role_permissions (role_id, permission_id)
-- SELECT r.role_id, p.permission_id
-- FROM indicolors.roles r
-- CROSS JOIN indicolors.permissions p
-- WHERE r.name = 'Operador'
--   AND p.code = 'orders.view'
-- ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- 6) Revocar un permiso de un rol
-- -----------------------------------------------------------------------------
-- DELETE FROM indicolors.role_permissions rp
-- USING indicolors.roles r, indicolors.permissions p
-- WHERE rp.role_id = r.role_id
--   AND rp.permission_id = p.permission_id
--   AND r.name = 'Operador'
--   AND p.code = 'orders.view';

-- -----------------------------------------------------------------------------
-- 7) Agregar un permiso nuevo al catálogo (si aún no existe)
-- -----------------------------------------------------------------------------
-- INSERT INTO indicolors.permissions (permission_id, code, name, module, description)
-- VALUES (
--     gen_random_uuid(),
--     'clients.view',
--     'Ver clientes',
--     'clientes',
--     'Listar y consultar clientes'
-- )
-- ON CONFLICT (code) DO NOTHING;
--
-- Luego asignarlo a un rol con el patrón del apartado 5.
-- =============================================================================
