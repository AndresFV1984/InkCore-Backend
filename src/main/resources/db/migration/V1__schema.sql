-- Schema indicolors + extensión pgcrypto (gen_random_uuid).
-- Convención Flyway: un archivo por tabla a partir de V2 (ver docs/ESTRUCTURA_INDICORE.md).
-- Asignación de permisos de negocio: scripts/postgres/assign-role-permissions.sql (manual).

CREATE SCHEMA IF NOT EXISTS indicolors;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
