package com.retrocore.mod2.login.domain.service;

import com.retrocore.mod2.login.domain.User;
import com.retrocore.mod2.login.domain.UserRepositoryPort;
import com.retrocore.mod2.login.domain.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class AuthenticateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public User authenticate(String username, String rawPassword) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Usuário ou senha inválidos."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Usuário ou senha inválidos.");
        }

        return user;
    }
}
