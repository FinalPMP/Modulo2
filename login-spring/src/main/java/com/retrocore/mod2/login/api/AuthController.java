package com.retrocore.mod2.login.api;

import com.retrocore.mod2.login.api.dto.LoginRequest;
import com.retrocore.mod2.login.api.dto.LoginResponse;
import com.retrocore.mod2.login.api.dto.RegisterRequest;
import com.retrocore.mod2.login.api.dto.RegisterResponse;
import com.retrocore.mod2.login.application.LoginService;
import com.retrocore.mod2.login.application.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final RegisterService registerService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(registerService.register(request));
    }
}
