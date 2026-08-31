package com.VanControl.VanControl.veiculo.domain.dto.response;

public record VeiculoResponseDto(
        String placa,
        String marca,
        String modelo,
        int ano,
        int capacidade,
        String status
) {
}
