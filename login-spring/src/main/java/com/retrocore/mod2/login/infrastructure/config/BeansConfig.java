package com.retrocore.mod2.login.infrastructure.config;

import com.retrocore.mod2.login.domain.UserRepositoryPort;
import com.retrocore.mod2.login.domain.service.AuthenticateUserUseCase;
import com.retrocore.mod2.login.domain.service.RegisterUserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeansConfig {

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepositoryPort repositoryPort,
                                                           PasswordEncoder passwordEncoder) {
        return new AuthenticateUserUseCase(repositoryPort, passwordEncoder);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepositoryPort repositoryPort,
                                                   PasswordEncoder passwordEncoder) {
        return new RegisterUserUseCase(repositoryPort, passwordEncoder);
    }
}
