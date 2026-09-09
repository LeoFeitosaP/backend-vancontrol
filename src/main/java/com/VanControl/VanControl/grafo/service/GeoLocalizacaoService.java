package com.VanControl.VanControl.grafo.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.grafo.domain.dto.Coordenadas;
import com.VanControl.VanControl.grafo.domain.dto.external.NominatimResponse;
import com.VanControl.VanControl.grafo.domain.dto.external.OsrmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GeoLocalizacaoService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Coordenadas geocodificar(String endereco, String cep) {
        String query = URLEncoder.encode(endereco + " " + cep, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?q=" + query + "&format=json&limit=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "VanControl/1.0 (contato@vancontrol.com)");
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<NominatimResponse[]> resp =
                restTemplate.exchange(url, HttpMethod.GET, req, NominatimResponse[].class);

        if (resp.getBody() == null || resp.getBody().length == 0) {
            throw new BadRequestException("Não foi possível geocodificar o endereço informado");
        }
        var r = resp.getBody()[0];
        return new Coordenadas(Double.parseDouble(r.lat()), Double.parseDouble(r.lon()));
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

    private double distanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // raio da Terra em km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

