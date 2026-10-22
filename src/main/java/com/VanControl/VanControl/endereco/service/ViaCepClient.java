package com.VanControl.VanControl.endereco.service;

import com.VanControl.VanControl.endereco.domain.dto.CepResponseDto;
import com.VanControl.VanControl.endereco.exceptions.EnderecoException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Set;

@Component
public class ViaCepClient {

    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF",
            "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS",
            "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private final RestClient client;

    @Autowired
    public ViaCepClient(
            @Value("${integracao.viacep.url:https://viacep.com.br/ws}")
            String url,

            @Value("${integracao.viacep.connect-timeout-ms:2000}")
            int connectTimeout,

            @Value("${integracao.viacep.read-timeout-ms:3000}")
            int readTimeout
    ) {
        if (connectTimeout <= 0 || readTimeout <= 0) {
            throw new IllegalArgumentException(
                    "Timeouts do ViaCEP devem ser positivos"
            );
        }

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        this.client = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public ViaCepClient(RestClient client) {
        this.client = client;
    }

    public CepResponseDto consultar(String entrada) {
        String cep = normalizarCep(entrada);

        ViaCepResponse resposta;

        try {
            resposta = client.get()
                    .uri("/{cep}/json/", cep)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ViaCepResponse.class);

        } catch (ResourceAccessException ex) {
            throw new EnderecoException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Consulta de CEP indisponível. Tente novamente mais tarde"
            );

        } catch (RestClientResponseException ex) {
            throw new EnderecoException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Consulta de CEP indisponível. Tente novamente mais tarde"
            );

        } catch (RestClientException ex) {
            throw respostaInvalida();
        }

        if (resposta == null) {
            throw respostaInvalida();
        }

        if (Boolean.TRUE.equals(resposta.erro())) {
            throw new EnderecoException(
                    HttpStatus.NOT_FOUND,
                    "CEP não encontrado"
            );
        }

        if (resposta.cep() == null
                || !resposta.cep().matches("[0-9]{5}-?[0-9]{3}")
                || !cep.equals(resposta.cep().replace("-", ""))
                || !campoValido(resposta.localidade(), 100, true)
                || resposta.uf() == null
                || !UFS.contains(resposta.uf())
                || !campoValido(resposta.logradouro(), 200, false)
                || !campoValido(resposta.bairro(), 100, false)) {
            throw respostaInvalida();
        }

        return new CepResponseDto(
                cep,
                limpar(resposta.logradouro()),
                limpar(resposta.bairro()),
                resposta.localidade().trim(),
                resposta.uf(),
                limpar(resposta.complemento())
        );
    }

    public static String normalizarCep(String entrada) {
        if (entrada == null
                || !entrada.matches("[0-9]{5}-?[0-9]{3}")) {
            throw new EnderecoException(
                    HttpStatus.BAD_REQUEST,
                    "CEP deve conter 8 dígitos, com hífen opcional"
            );
        }

        return entrada.replace("-", "");
    }

    private static boolean campoValido(
            String valor,
            int tamanho,
            boolean obrigatorio
    ) {
        return valor == null
                ? !obrigatorio
                : valor.length() <= tamanho
                && (!obrigatorio || !valor.isBlank());
    }

    private static String limpar(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : valor.trim();
    }

    private static EnderecoException respostaInvalida() {
        return new EnderecoException(
                HttpStatus.BAD_GATEWAY,
                "ViaCEP retornou uma resposta inválida"
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ViaCepResponse(
            String cep,
            String logradouro,
            String complemento,
            String bairro,
            String localidade,
            String uf,
            Boolean erro
    ) {
    }
}