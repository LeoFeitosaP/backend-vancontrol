package com.VanControl.VanControl.grafo.service;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;

final class ClienteGeograficoHttp {

    private ClienteGeograficoHttp() {
    }

    static RestClient criar(String url, String userAgent, int timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Timeout geográfico deve ser positivo");
        }

        if (url == null || url.isBlank()) {
            return null;
        }

        URI uri = URI.create(url);

        if (uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("URL do provedor geográfico inválida");
        }

        if (userAgent == null || userAgent.isBlank()
                || userAgent.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Informe o User-Agent da integração");
        }

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return RestClient.builder()
                .baseUrl(url.replaceAll("/+$", ""))
                .defaultHeader("User-Agent", userAgent)
                .requestFactory(factory)
                .build();
    }
}