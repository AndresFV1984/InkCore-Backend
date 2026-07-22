package com.inkcore.infrastructure.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;

/**
 * Crea un DataSource dedicado para Flyway sólo si se definen ambas variables
 * de entorno FLYWAY_MIGRATION_USER y FLYWAY_MIGRATION_PASSWORD.
 * Esto evita que Flyway intente conectarse con un usuario (p. ej. indicolors_owner)
 * sin contraseña correctamente configurada en el entorno.
 */
@Configuration
@ConditionalOnExpression("'${FLYWAY_MIGRATION_USER:}' != '' and '${FLYWAY_MIGRATION_PASSWORD:}' != ''")
public class FlywayAdminDataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(FlywayAdminDataSourceConfig.class);

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @FlywayDataSource
    public DataSource flywayDataSource(@Value("${FLYWAY_MIGRATION_USER}") String flywayUser,
                                      @Value("${FLYWAY_MIGRATION_PASSWORD}") String flywayPassword) {
        log.info("Inicializando Flyway DataSource usando usuario de migración: {}", flywayUser);

        // Construimos el DataSource sin referenciar la clase del driver en tiempo de compilación
        DataSource ds = DataSourceBuilder.create()
                .driverClassName(this.driverClassName)
                .url(this.datasourceUrl)
                .username(flywayUser)
                .password(flywayPassword)
                .build();
        return ds;
    }
}

