package com.VanControl.VanControl.grafo.domain.dto;

import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Distancia;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Ponto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class GeografiaPersistida {

    private GeografiaPersistida() {
    }

    public enum Tipo {
        PASSAGEIRO,
        VEICULO,
        VIAGEM
    }

    public record Localizacao(
            UUID id,
            Endereco endereco,
            String origemEndereco,
            String justificativa,
            long revisao,
            String estado,
            Ponto coordenadas,
            String mensagem,
            String atribuicao,
            Instant consultadoEm
    ) {
    }

    public record Calculo(
            UUID origemId,
            UUID destinoId,
            long origemRevisao,
            long destinoRevisao,
            boolean permitirEstimativa,
            Distancia resultado,
            Instant calculadoEm,
            Instant expiraEm
    ) {
    }

    public record RevisarOrigem(
            @NotNull @Valid EnderecoRequestDto endereco,
            @NotBlank @Size(max = 500) String justificativa
    ) {
    }
}