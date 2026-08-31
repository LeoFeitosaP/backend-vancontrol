package com.VanControl.VanControl.veiculo.domain.dto.response;

import java.util.UUID;

public record AdminVeiculoResponseDto(
        UUID id,
        String placa,
        String marca,
        String modelo,
        int ano,
        int capacidade,
        String renavam,
        String status
) {
}
