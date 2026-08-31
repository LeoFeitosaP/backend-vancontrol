package com.VanControl.VanControl.user.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequestDto(
        @Email(message = "Insira um email válido")
        @NotBlank(message = "Insira seu email")
        String email,

        @NotBlank(message = "Insira o PIN enviado no seu email")
        String pin,

        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", message = "A senha deve conter pelo menos 8 caracteres, incluindo letras e números")
        @NotBlank(message = "Insira sua senha")
        String newPassword) {
}
