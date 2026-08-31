package com.VanControl.VanControl.veiculo.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AtualizarStatusVeiculoRequestDto(
        @NotBlank(message = "Insira a Placa do Veiculo")
        @Pattern(regexp = "^[A-Z]{3}-[0-9][A-Z0-9][0-9]{2}$", message = "A placa deve seguir estar no formato Mercosul ou antigo")
        String placa,
        @NotBlank(message = "Insira o status do veiculo")
        String status
) {
}
