package com.inkcore;

import com.inkcore.infrastructure.config.PasswordPolicyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@EnableConfigurationProperties(PasswordPolicyProperties.class)
public class InkCoreBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InkCoreBackendApplication.class, args);
    }
}
