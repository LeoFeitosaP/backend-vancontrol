package com.VanControl.VanControl.veiculo.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CadastrarVeiculoRequestDto(
        @NotBlank(message = "Insira a Placa do Veicullo")
        @Pattern(regexp = "^[A-Z]{3}-[0-9][A-Z0-9][0-9]{2}$", message = "A placa deve seguir estar no formato Mercosul ou antigo")
        String placa,
        @NotBlank(message = "Insira a marca do veiculo")
        String marca,
        @NotBlank(message = "Insira o modelo do veiculo")
        String modelo,
        @NotNull(message = "Insira o ano do veiculo")
        int ano,
        @NotNull(message = "Insira a capacidade do veiculo")
        int capacidade,
        @NotBlank(message = "Insira o renavam do veiculo")
        @Pattern(regexp = "^[0-9]{11}$", message = "O renavam deve conter exatamente 11 dígitos numéricos")
        String renavam,
        @NotBlank(message = "Insira o status do veiculo")
        String status
) {
}
