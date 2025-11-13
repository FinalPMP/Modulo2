package com.retrocore.mod2.login.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {

    private Long id;
    private String username;
    private String token;
}
