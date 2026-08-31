package com.VanControl.VanControl.rota.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

public record CadastrarRotaRequestDto(
        @NotBlank(message = "Insira a descrição da rota")
        String descricao,

        @NotBlank(message = "Insira o destino da rota")
        String destino,

        @NotNull(message = "Insira a distancia da rota")
        @Positive(message = "A distancia deve ser maior que zero")
        Double distancia,

        @NotNull(message = "Insira o tempo estimado da rota")
        @JsonFormat(pattern = "HH:mm")
        LocalTime tempoEstimado
) {


}
