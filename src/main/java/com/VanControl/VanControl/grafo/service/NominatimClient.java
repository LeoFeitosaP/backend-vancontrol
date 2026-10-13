package com.VanControl.VanControl.grafo.service;

import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Geocodificacao;
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

import java.net.URI;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.EstadoGeocodificacao.*;

@Component
public class NominatimClient {

    private static final String ATRIBUICAO = "© OpenStreetMap contributors";

    private final RestClient client;

    @Autowired
    public NominatimClient(
            @Value("${integracao.nominatim.url:}") String url,
            @Value("${integracao.nominatim.user-agent:VanControl}") String userAgent,
            @Value("${integracao.geografica.timeout-ms:3000}") int timeoutMs
    ) {
        // O fluxo trata endereços residenciais. Use instância própria ou contratada.
        if (url != null && !url.isBlank()
                && "nominatim.openstreetmap.org".equalsIgnoreCase(URI.create(url).getHost())) {
            this.client = null;
        } else {
            this.client = ClienteGeograficoHttp.criar(url, userAgent, timeoutMs);
        }
    }

    public NominatimClient(RestClient client) {
        this.client = client;
    }

    public Geocodificacao consultar(Endereco endereco) {
        if (!completo(endereco)) {
            return resultado(
                    ENDERECO_INCOMPLETO,
                    null,
                    "Informe CEP, logradouro, número, bairro, cidade e UF"
            );
        }

        if (client == null) {
            return resultado(
                    NAO_CONFIGURADO,
                    null,
                    "Configure uma instância Nominatim adequada a endereços residenciais"
            );
        }

        // Complemento identifica unidade interna; não localiza a entrada do imóvel.
        String busca = String.join(
                ", ",
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf(),
                endereco.getCep(),
                "Brasil"
        );

        Candidato[] resposta;

        try {
            resposta = client.get()
                    .uri(builder -> builder.path("/search")
                            .queryParam("q", "{busca}")
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1)
                            .queryParam("limit", 5)
                            .queryParam("countrycodes", "br")
                            .queryParam("accept-language", "pt-BR")
                            .build(busca))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Candidato[].class);

        } catch (ResourceAccessException | RestClientResponseException ex) {
            return resultado(
                    INDISPONIVEL,
                    null,
                    "Geocodificador indisponível; tente novamente"
            );

        } catch (RestClientException ex) {
            return resultado(
                    RESPOSTA_INVALIDA,
                    null,
                    "Geocodificador retornou resposta inválida"
            );
        }

        if (resposta == null) {
            return resultado(
                    RESPOSTA_INVALIDA,
                    null,
                    "Geocodificador retornou resposta vazia"
            );
        }

        if (resposta.length == 0) {
            return resultado(
                    NAO_ENCONTRADA,
                    null,
                    "Endereço não localizado pelo geocodificador"
            );
        }

        var pontos = new HashSet<Ponto>();

        for (Candidato candidato : resposta) {
            Ponto ponto = coordenadas(candidato);

            if (ponto == null || candidato.address() == null) {
                return resultado(
                        RESPOSTA_INVALIDA,
                        null,
                        "Candidato com dados inválidos"
                );
            }

            if (compativel(endereco, candidato.address())) {
                pontos.add(ponto);
            }
        }

        if (pontos.size() > 1 || resposta.length >= 5) {
            return resultado(
                    AMBIGUA,
                    null,
                    "Busca sem resultado único; revise o endereço"
            );
        }

        if (pontos.isEmpty()) {
            return resultado(
                    IMPRECISA,
                    null,
                    "Nenhum resultado confirma logradouro, número, cidade, UF e CEP"
            );
        }

        return resultado(
                CONFIRMADA,
                pontos.iterator().next(),
                "Endereço confirmado pelo provedor"
        );
    }

    private static boolean completo(Endereco e) {
        return e != null && presente(e.getLogradouro()) && presente(e.getNumero())
                && presente(e.getBairro()) && presente(e.getCidade())
                && e.getCep() != null && e.getCep().matches("[0-9]{8}")
                && e.getUf() != null && e.getUf().matches("[A-Z]{2}");
    }

    private static boolean compativel(Endereco e, Map<String, String> a) {
        boolean cidade = List.of("city", "town", "village", "municipality").stream()
                .anyMatch(chave -> igual(e.getCidade(), a.get(chave)));

        String cep = a.getOrDefault("postcode", "");

        return "br".equalsIgnoreCase(a.get("country_code"))
                && igual(e.getLogradouro(), a.get("road"))
                && igual(e.getNumero(), a.get("house_number"))
                && cidade
                && ("BR-" + e.getUf()).equalsIgnoreCase(a.get("ISO3166-2-lvl4"))
                && cep != null && e.getCep().equals(cep.replace("-", "").trim());
    }

    private static Ponto coordenadas(Candidato c) {
        if (c == null || c.lat() == null || c.lon() == null) {
            return null;
        }

        try {
            var ponto = new Ponto(
                    Double.valueOf(c.lat()),
                    Double.valueOf(c.lon())
            );

            return ponto.valido() ? ponto : null;

        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean presente(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static boolean igual(String a, String b) {
        return presente(a) && presente(b) && normalizar(a).equals(normalizar(b));
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private static Geocodificacao resultado(
            com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.EstadoGeocodificacao estado,
            Ponto ponto,
            String mensagem
    ) {
        return new Geocodificacao(estado, ponto, mensagem, ATRIBUICAO);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidato(
            String lat,
            String lon,
            Map<String, String> address
    ) {
    }
}