package com.VanControl.VanControl.motorista.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.YearMonth;

public record CadastrarMotoristaRequestDto(
        @Pattern(regexp = "^[A-ZÀ-Ÿ][a-zà-ÿ]+(?: [A-ZÀ-Ÿ][a-zà-ÿ]+)*$", message = "Insira seu nome completo, iniciando com letra maiúscula")
        @NotBlank(message = "Insira o nome do motorista")
        String nome,
        @Email(message = "Insira um email válido")
        @NotBlank(message = "Insira o email do motorista")
        String email,
        @Pattern(regexp = "^[0-9]{11}$", message = "Insira a CNH com 11 dígitos numéricos")
        @NotBlank(message = "Insira a CNH do motorista")
        String cnh,
        @Pattern(regexp = "(A|B|AB|C|D|E|AC|AD|AE)", message = "Insira a categoria da CNH")
        @NotBlank(message = "Insira a categoria da CNH do motorista")
        String categoriaCnh,
        @JsonFormat(pattern = "yyyy/MM")
        @Future(message = "A CNH não pode estar vencida")
        @NotNull(message = "Insira a data de validade da CNH do motorista")
        YearMonth dataValidadeCnh,
        @CPF(message = "CPF inválido")
        @NotBlank(message = "Insira o CPF do motorista")
        String cpf,
        @Pattern(regexp = "^\\([0-9]{2}\\)\\s?[0-9]{5}-[0-9]{4}$", message = "Insira o telefone no formato 12345-6789")
        @NotBlank(message = "Insira o telefone do motorista")
        String telefone,
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", message = "A senha deve conter pelo menos 8 caracteres, incluindo letras e números")
        @NotBlank(message = "Insira sua senha")
        String password
) {
}
