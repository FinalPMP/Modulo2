package com.retrocore.mod2.login.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "username é obrigatório")
    private String username;

    @NotBlank(message = "password é obrigatório")
    private String password;
}
