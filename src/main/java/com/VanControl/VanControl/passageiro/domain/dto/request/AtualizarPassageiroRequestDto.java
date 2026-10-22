package com.VanControl.VanControl.passageiro.domain.dto.request;

import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record AtualizarPassageiroRequestDto(
        @Pattern(
                regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ ]+$",
                message = "O nome deve conter apenas letras e espaços"
        )
        String nome,

        @Pattern(
                regexp = "^\\([0-9]{2}\\)\\s?[0-9]{5}-[0-9]{4}$",
                message = "Insira o telefone no formato 12345-6789"
        )
        String telefone,

        @Email(message = "Insira um email válido")
        String email,

        @Pattern(
                regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ ]+$",
                message = "O nome da Instituição de ensino deve conter apenas letras e espaços"
        )
        String intituicaoEnsino,

        @Pattern(
                regexp = "^(Manhã|Tarde|Noite)$",
                message = "O turno deve ser 'Manhã', 'Tarde' ou 'Noite'"
        )
        String turno,

        @Valid
        @JsonAlias("Endereco")
        EnderecoRequestDto endereco
) {
}