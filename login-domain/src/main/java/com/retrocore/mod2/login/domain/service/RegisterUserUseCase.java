package com.retrocore.mod2.login.domain.service;

import com.retrocore.mod2.login.domain.User;
import com.retrocore.mod2.login.domain.UserRepositoryPort;
import com.retrocore.mod2.login.domain.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String username, String rawPassword, String fullName) {

        if (userRepository.existsByUsername(username)) {
            throw new InvalidCredentialsException("Usuário já existe.");
        }

        var user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role("USER")
                .build();

        return userRepository.save(user);
    }
}
