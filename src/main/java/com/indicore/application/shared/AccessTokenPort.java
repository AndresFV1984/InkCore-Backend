package com.indicore.application.shared;

import java.util.List;

/**
 * Puerto de salida: emisión de access token (implementado con JWT en infraestructura).
 */
public interface AccessTokenPort {

    String createAccessToken(String subject, long tokenVersion, List<String> roleCodes);

    long getExpirationSeconds();
}
