package com.VanControl.VanControl.user.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDto(
        @Email(message = "Insira um email válido")
        @NotBlank(message = "Insira seu email")
        String email)
{
}
