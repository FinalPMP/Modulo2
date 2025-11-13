package com.retrocore.mod2.login.application;

import com.retrocore.mod2.login.api.dto.RegisterRequest;
import com.retrocore.mod2.login.api.dto.RegisterResponse;
import com.retrocore.mod2.login.domain.User;
import com.retrocore.mod2.login.domain.service.RegisterUserUseCase;
import com.retrocore.mod2.login.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final RegisterUserUseCase registerUserUseCase;
    private final JwtService jwtService;

    public RegisterResponse register(RegisterRequest request) {
        User user = registerUserUseCase.register(
                request.getUsername(),
                request.getPassword(),
                request.getFullName()
        );

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new RegisterResponse(user.getId(), user.getUsername(), token);
    }
}
