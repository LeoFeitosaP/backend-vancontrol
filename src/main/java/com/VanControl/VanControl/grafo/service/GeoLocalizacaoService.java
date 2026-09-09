package com.VanControl.VanControl.grafo.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.grafo.domain.dto.Coordenadas;
import com.VanControl.VanControl.grafo.domain.dto.external.NominatimResponse;
import com.VanControl.VanControl.grafo.domain.dto.external.OsrmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeoLocalizacaoService {

    private final RestTemplate restTemplate;

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    public Coordenadas geocodificar(String endereco, String cep) {
        // 1ª tentativa: endereço completo
        Coordenadas resultado = tentarGeocodificar(endereco);

        // 2ª tentativa (fallback): endereço + cep, caso o endereço sozinho seja ambíguo demais
        if (resultado == null && cep != null && !cep.isBlank()) {
            resultado = tentarGeocodificar(endereco + ", " + cep);
        }

        if (resultado == null) {
            throw new BadRequestException(
                    "Não foi possível geocodificar o endereço informado: " + endereco);
        }
        return resultado;
    }

    public double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        try {
            String url = String.format(Locale.US,
                    "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                    lon1, lat1, lon2, lat2);
            var resp = restTemplate.getForObject(url, OsrmResponse.class);
            double metros = resp.routes().get(0).distance();
            return metros / 1000.0;
        } catch (Exception e) {
            // fallback gratuito e local, sem depender de rede
            return distanciaHaversine(lat1, lon1, lat2, lon2);
        }
    }

    private Coordenadas tentarGeocodificar(String textoBusca) {
        URI uri = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                .queryParam("q", textoBusca)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .queryParam("countrycodes", "br")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "VanControl/1.0 (contato@vancontrol.com)");
        HttpEntity<Void> req = new HttpEntity<>(headers);

        try {
            log.info("Chamando Nominatim: {}", uri);
            var resp = restTemplate.exchange(uri, HttpMethod.GET, req, NominatimResponse[].class);

            int qtd = resp.getBody() == null ? 0 : resp.getBody().length;
            log.info("Nominatim respondeu {} com {} resultado(s)", resp.getStatusCode(), qtd);

            if (qtd == 0) return null;

            var r = resp.getBody()[0];
            return new Coordenadas(Double.parseDouble(r.lat()), Double.parseDouble(r.lon()));
        } catch (Exception e) {
            log.error("Erro ao chamar Nominatim para '{}': {}", textoBusca, e.getMessage());
            return null;
        }
    }

    private double distanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // raio da Terra em km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}