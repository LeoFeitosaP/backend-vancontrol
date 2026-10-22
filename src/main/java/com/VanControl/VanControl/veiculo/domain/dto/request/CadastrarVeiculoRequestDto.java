package com.VanControl.VanControl.veiculo.domain.dto.request;

import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CadastrarVeiculoRequestDto(

        @NotBlank
        @Pattern(
                regexp = "^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$",
                message = "Placa inválida"
        )
        String placa,

        @NotBlank
        String marca,

        @NotBlank
        String modelo,

        @Positive
        int ano,

        @Positive
        int capacidade,

        @NotBlank
        @Pattern(
                regexp = "^[0-9]{11}$",
                message = "RENAVAM deve conter 11 dígitos"
        )
        String renavam,

        @NotBlank
        @Pattern(
                regexp = "(?i)^(DISPONIVEL|MANUTENCAO|INATIVO)$",
                message = "Status inválido"
        )
        String status,

        @Valid
        @NotNull(message = "Informe o endereço da base da van")
        EnderecoRequestDto endereco
) {
}