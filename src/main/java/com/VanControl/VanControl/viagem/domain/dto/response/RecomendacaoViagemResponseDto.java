package com.VanControl.VanControl.viagem.domain.dto.response;

import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.MetodoDistancia;

import java.time.Instant;
import java.time.LocalDateTime;

public record RecomendacaoViagemResponseDto(
        ViagemResponseDto viagem,
        String destino,
        LocalDateTime chegadaPrevista,
        long vagasDisponiveis,
        Distancia distancia,
        Double pontuacao
) {

    public record Distancia(
            String estado,
            MetodoDistancia metodo,
            Double distanciaKm,
            String estadoOrigem,
            String estadoPassageiro,
            Instant calculadoEm,
            Instant expiraEm,
            String atribuicao
    ) {
    }
}