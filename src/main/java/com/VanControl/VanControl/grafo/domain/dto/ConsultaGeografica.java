package com.VanControl.VanControl.grafo.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public final class ConsultaGeografica {

    private ConsultaGeografica() {
    }

    public enum EstadoGeocodificacao {
        CONFIRMADA, NAO_ENCONTRADA, AMBIGUA, IMPRECISA,
        ENDERECO_INCOMPLETO, INDISPONIVEL, RESPOSTA_INVALIDA, NAO_CONFIGURADO
    }

    public enum EstadoDistancia {
        CALCULADA, ESTIMADA, SEM_ROTA, PONTO_FORA_DA_VIA,
        INDISPONIVEL, RESPOSTA_INVALIDA, NAO_CONFIGURADO
    }

    public enum MetodoDistancia {
        RODOVIARIA, HAVERSINE
    }

    public record Ponto(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude
    ) {
        public boolean valido() {
            return latitude != null && longitude != null
                    && Double.isFinite(latitude) && Double.isFinite(longitude)
                    && latitude >= -90 && latitude <= 90
                    && longitude >= -180 && longitude <= 180;
        }
    }

    public record Geocodificacao(
            EstadoGeocodificacao estado,
            Ponto coordenadas,
            String mensagem,
            String atribuicao
    ) {
    }

    public record Distancia(
            EstadoDistancia estado,
            MetodoDistancia metodo,
            Double distanciaKm,
            String motivo,
            String atribuicao
    ) {
    }

    public record DistanciaRequest(
            @NotNull @Valid Ponto origem,
            @NotNull @Valid Ponto destino,
            boolean permitirEstimativa
    ) {
    }
}