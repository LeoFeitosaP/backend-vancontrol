package com.VanControl.VanControl.grafo.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Distancia;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.EstadoDistancia;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Ponto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Locale;

import static com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.EstadoDistancia.*;
import static com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.MetodoDistancia.*;

@Component
public class OsrmClient {

    private static final String ATRIBUICAO = "OSRM; © OpenStreetMap contributors";

    private final RestClient client;
    private final double limiteAjusteMetros;

    @Autowired
    public OsrmClient(
            @Value("${integracao.osrm.url:}") String url,
            @Value("${integracao.nominatim.user-agent:VanControl}") String userAgent,
            @Value("${integracao.geografica.timeout-ms:3000}") int timeoutMs,
            @Value("${integracao.osrm.limite-ajuste-metros:100}") double limiteAjusteMetros
    ) {
        this(
                ClienteGeograficoHttp.criar(url, userAgent, timeoutMs),
                limiteAjusteMetros
        );
    }

    public OsrmClient(RestClient client, double limiteAjusteMetros) {
        if (!Double.isFinite(limiteAjusteMetros) || limiteAjusteMetros <= 0) {
            throw new IllegalArgumentException(
                    "Limite de ajuste à via deve ser positivo"
            );
        }

        this.client = client;
        this.limiteAjusteMetros = limiteAjusteMetros;
    }

    public Distancia consultar(
            Ponto origem,
            Ponto destino,
            boolean permitirEstimativa
    ) {
        if (origem == null || destino == null
                || !origem.valido() || !destino.valido()) {
            throw new BadRequestException(
                    "Coordenadas de origem ou destino inválidas"
            );
        }

        if (client == null) {
            return falha(
                    NAO_CONFIGURADO,
                    origem,
                    destino,
                    permitirEstimativa
            );
        }

        // O OSRM recebe longitude,latitude e preserva a ordem origem -> destino.
        String caminho = String.format(
                Locale.ROOT,
                "/route/v1/driving/%.7f,%.7f;%.7f,%.7f",
                origem.longitude(),
                origem.latitude(),
                destino.longitude(),
                destino.latitude()
        );

        Resposta resposta;

        try {
            resposta = client.get()
                    .uri(caminho + "?overview=false&alternatives=false&steps=false")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Resposta.class);

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400) {
                try {
                    Resposta erro = ex.getResponseBodyAs(Resposta.class);
                    Distancia semRota = ausenciaDeRota(erro);

                    if (semRota != null) {
                        return semRota;
                    }

                } catch (RestClientException ignored) {
                    // Corpo ilegível: mantém a classificação de resposta inválida.
                }

                return falha(
                        RESPOSTA_INVALIDA,
                        origem,
                        destino,
                        permitirEstimativa
                );
            }

            return falha(
                    INDISPONIVEL,
                    origem,
                    destino,
                    permitirEstimativa
            );

        } catch (ResourceAccessException ex) {
            return falha(
                    INDISPONIVEL,
                    origem,
                    destino,
                    permitirEstimativa
            );

        } catch (RestClientException ex) {
            return falha(
                    RESPOSTA_INVALIDA,
                    origem,
                    destino,
                    permitirEstimativa
            );
        }

        Distancia semRota = ausenciaDeRota(resposta);

        if (semRota != null) {
            return semRota;
        }

        if (resposta == null || !"Ok".equals(resposta.code())
                || resposta.routes() == null || resposta.routes().isEmpty()
                || resposta.routes().getFirst() == null
                || !naoNegativo(resposta.routes().getFirst().distance())
                || resposta.waypoints() == null || resposta.waypoints().size() != 2
                || resposta.waypoints().stream()
                .anyMatch(p -> p == null || !naoNegativo(p.distance()))) {
            return falha(
                    RESPOSTA_INVALIDA,
                    origem,
                    destino,
                    permitirEstimativa
            );
        }

        if (resposta.waypoints().stream()
                .anyMatch(p -> p.distance() > limiteAjusteMetros)) {
            return new Distancia(
                    PONTO_FORA_DA_VIA,
                    null,
                    null,
                    "AJUSTE_A_VIA_EXCEDE_LIMITE",
                    ATRIBUICAO
            );
        }

        return new Distancia(
                CALCULADA,
                RODOVIARIA,
                resposta.routes().getFirst().distance() / 1000.0,
                null,
                ATRIBUICAO
        );
    }

    private static Distancia ausenciaDeRota(Resposta resposta) {
        if (resposta != null && "NoRoute".equals(resposta.code())) {
            return new Distancia(
                    SEM_ROTA,
                    null,
                    null,
                    "OSRM_NO_ROUTE",
                    ATRIBUICAO
            );
        }

        if (resposta != null && "NoSegment".equals(resposta.code())) {
            return new Distancia(
                    PONTO_FORA_DA_VIA,
                    null,
                    null,
                    "OSRM_NO_SEGMENT",
                    ATRIBUICAO
            );
        }

        return null;
    }

    private static boolean naoNegativo(Double valor) {
        return valor != null && Double.isFinite(valor) && valor >= 0;
    }

    private static Distancia falha(
            EstadoDistancia estado,
            Ponto origem,
            Ponto destino,
            boolean permitirEstimativa
    ) {
        if (permitirEstimativa) {
            return new Distancia(
                    ESTIMADA,
                    HAVERSINE,
                    haversine(origem, destino),
                    estado.name(),
                    "Estimativa em linha reta; não representa trajeto rodoviário"
            );
        }

        return new Distancia(
                estado,
                null,
                null,
                estado.name(),
                ATRIBUICAO
        );
    }

    private static double haversine(Ponto origem, Ponto destino) {
        double dLat = Math.toRadians(
                destino.latitude() - origem.latitude()
        );

        double dLon = Math.toRadians(
                destino.longitude() - origem.longitude()
        );

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(origem.latitude()))
                * Math.cos(Math.toRadians(destino.latitude()))
                * Math.pow(Math.sin(dLon / 2), 2);

        a = Math.max(0, Math.min(1, a));

        return 6371.0088 * 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resposta(
            String code,
            List<Rota> routes,
            List<PontoNaVia> waypoints
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rota(Double distance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PontoNaVia(Double distance) {
    }
}