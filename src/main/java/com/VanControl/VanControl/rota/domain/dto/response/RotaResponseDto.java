package com.VanControl.VanControl.rota.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record RotaResponseDto(
        String codigoRota,
        String descricao,
        String destino,
        Double distancia,

        @JsonFormat(pattern = "HH:mm")
        LocalTime tempoEstimado

) {
}
