package com.retrocore.mod2.login.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username é obrigatório")
    private String username;

    @NotBlank(message = "password é obrigatório")
    private String password;

    @NotBlank(message = "fullName é obrigatório")
    private String fullName;
}
