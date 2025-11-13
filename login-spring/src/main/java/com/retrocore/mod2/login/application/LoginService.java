package com.retrocore.mod2.login.application;

import com.retrocore.mod2.login.api.dto.LoginRequest;
import com.retrocore.mod2.login.api.dto.LoginResponse;
import com.retrocore.mod2.login.domain.User;
import com.retrocore.mod2.login.domain.service.AuthenticateUserUseCase;
import com.retrocore.mod2.login.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = authenticateUserUseCase.authenticate(
                request.getUsername(),
                request.getPassword()
        );

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(user.getUsername(), token);
    }
}
