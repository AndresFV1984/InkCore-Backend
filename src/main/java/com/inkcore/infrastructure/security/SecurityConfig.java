package com.inkcore.infrastructure.security;

import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Seguridad con JWT (OAuth2 Resource Server).
 * Públicos: POST /api/v1/users/login, POST /api/v1/auth/login, POST /api/v1/auth/refresh y documentación.
 * CORS: whitelist vía {@code app.cors} (ver docs/CORS.md); OPTIONS permitido sin JWT.
 * Perfil {@code security.dev.permit-all=true} desactiva la autenticación (solo desarrollo).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.dev.permit-all:false}")
    private boolean permitAll;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtAuthoritiesConverter());
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> jwtAuthoritiesConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : claimAsStringList(jwt, "roles")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            for (String permission : claimAsStringList(jwt, "permissions")) {
                authorities.add(new SimpleGrantedAuthority("PERMISSION_" + permission));
            }
            return authorities;
        };
    }

    private static List<String> claimAsStringList(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        if (claim instanceof Collection<?> collection) {
            List<String> values = new ArrayList<>();
            for (Object item : collection) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
            return values;
        }
        return List.of();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ApiResponseFactory responseFactory,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                responseFactory.writeError(
                                        request,
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "No autenticado"
                                ))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                responseFactory.writeError(
                                        request,
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "No tiene permiso para acceder a este recurso"
                                )));

        if (permitAll) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/error", "/error/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, authException) ->
                                responseFactory.writeError(
                                        request,
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "No autenticado"
                                ))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                responseFactory.writeError(
                                        request,
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "No tiene permiso para acceder a este recurso"
                                )));

        return http.build();
    }
}
