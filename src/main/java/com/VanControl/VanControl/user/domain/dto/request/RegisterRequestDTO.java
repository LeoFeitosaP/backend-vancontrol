package com.VanControl.VanControl.user.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.br.CPF;

public record RegisterRequestDTO(
        @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "O nome deve conter apenas letras e espaços")
        @NotBlank(message = "Insira seu nome completo")
        String name,
        @Email(message = "Insira um email válido")
        @NotBlank(message = "Insira seu email")
        String email,
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", message = "A senha deve conter pelo menos 8 caracteres, incluindo letras e números")
        @NotBlank(message = "Insira sua senha")
        String password,

        @CPF(message = "Insira um CPF válido")
        @NotBlank(message = "Insira seu CPF")
        String cpf,
        @Pattern(regexp = "^\\([0-9]{2}\\)\\s?[0-9]{5}-[0-9]{4}$", message = "Insira o telefone no formato 12345-6789")
        @NotBlank(message = "Insira seu telefone")
        String telefone,
        @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "O nome da instituição de ensino deve conter apenas letras e espaços")
        @NotBlank(message = "Insira o nome da instituição de ensino")
        String instituicaoEnsino,
        @Pattern(regexp = "^(Manhã|Tarde|Noite)$", message = "O turno deve ser 'Manhã', 'Tarde' ou 'Noite'")
        @NotBlank(message = "Insira seu turno")
        String turno,
        @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ0-9 ,.-]+$", message = "O endereço deve conter apenas letras, números e espaços. Use o seguinte formato: Rua Exemplo, 123, Bairro, Cidade, Estado")
        @NotBlank(message = "Insira seu endereço")
        String endereco,
        @Pattern(regexp = "^[0-9]{5}-[0-9]{3}$", message = "Insira o CEP no formato 12345-678")
        @NotBlank(message = "Insira seu CEP")
        String cep
) {
}
