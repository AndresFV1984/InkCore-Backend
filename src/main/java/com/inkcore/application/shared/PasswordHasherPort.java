package com.inkcore.application.shared;

/**
 * Puerto de salida: hash y verificación de contraseñas (implementado en infraestructura).
 */
public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
