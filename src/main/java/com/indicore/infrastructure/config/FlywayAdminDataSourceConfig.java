package com.indicore.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Permite que Flyway use un rol con privilegios (p. ej. indicore_admin) mientras la app
 * sigue usando {@code indicore_app}. Activar con variables de entorno FLYWAY_USER y FLYWAY_PASSWORD.
 */
@Configuration
@ConditionalOnProperty(name = "FLYWAY_USER")
public class FlywayAdminDataSourceConfig {

    @Bean
    @FlywayDataSource
    public DataSource flywayDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${FLYWAY_USER}") String username,
            @Value("${FLYWAY_PASSWORD:}") String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(2);
        dataSource.setPoolName("flywayAdminPool");
        return dataSource;
    }
}
