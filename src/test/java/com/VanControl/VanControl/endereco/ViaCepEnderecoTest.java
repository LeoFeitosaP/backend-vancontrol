package com.VanControl.VanControl.endereco;

import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.VanControl.VanControl.endereco.exceptions.EnderecoException;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.endereco.service.ViaCepClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ViaCepEnderecoTest {

    private MockRestServiceServer server;
    private ViaCepClient client;
    private EnderecoService service;

    private static final String URL =
            "https://viacep.test/ws/01001000/json/";

    private static final String RESPOSTA = """
            {
              "cep": "01001-000",
              "logradouro": "Praça da Sé",
              "bairro": "Sé",
              "localidade": "São Paulo",
              "uf": "SP",
              "complemento": "lado ímpar"
            }
            """;

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder()
                .baseUrl("https://viacep.test/ws");

        server = MockRestServiceServer.bindTo(builder).build();
        client = new ViaCepClient(builder.build());
        service = new EnderecoService(client);
    }

    @Test
    void consultaNormalizaCepEPreencheDados() {
        server.expect(requestTo(URL))
                .andRespond(
                        withSuccess(RESPOSTA, MediaType.APPLICATION_JSON)
                );

        var endereco = service.resolver(
                new EnderecoRequestDto(
                        "01001-000",
                        null,
                        "100",
                        "Bloco A",
                        null,
                        null,
                        null
                )
        );

        assertEquals("01001000", endereco.getCep());
        assertEquals("Praça da Sé", endereco.getLogradouro());
        assertEquals("100", endereco.getNumero());
        assertEquals("Bloco A", endereco.getComplemento());
        assertEquals("Sé", endereco.getBairro());
        assertEquals("São Paulo", endereco.getCidade());
        assertEquals("SP", endereco.getUf());

        assertTrue(
                endereco.formatar().endsWith("01001000, Brasil")
        );

        server.verify();
    }

    @Test
    void numeroObrigatorioAntesDeConsultar() {
        var erro = assertThrows(
                EnderecoException.class,
                () -> service.resolver(
                        new EnderecoRequestDto(
                                "01001000",
                                null,
                                " ",
                                null,
                                null,
                                null,
                                null
                        )
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        server.verify();
    }

    @Test
    void cepMalformadoNaoChamaProvedor() {
        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(
                        EnderecoException.class,
                        () -> client.consultar("01001ABC")
                ).getStatus()
        );

        server.verify();
    }

    @Test
    void cepInexistenteRetorna404() {
        server.expect(requestTo(URL))
                .andRespond(
                        withSuccess(
                                "{\"erro\":true}",
                                MediaType.APPLICATION_JSON
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                assertThrows(
                        EnderecoException.class,
                        () -> client.consultar("01001000")
                ).getStatus()
        );

        server.verify();
    }

    @Test
    void timeoutRetorna503() {
        server.expect(requestTo(URL))
                .andRespond(
                        withException(
                                new SocketTimeoutException("simulado")
                        )
                );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(
                        EnderecoException.class,
                        () -> client.consultar("01001000")
                ).getStatus()
        );

        server.verify();
    }

    @Test
    void erroHttpDoProvedorRetorna503() {
        server.expect(requestTo(URL))
                .andRespond(
                        withStatus(HttpStatus.TOO_MANY_REQUESTS)
                );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(
                        EnderecoException.class,
                        () -> client.consultar("01001000")
                ).getStatus()
        );

        server.verify();
    }

    @Test
    void jsonInvalidoRetorna502() {
        server.expect(requestTo(URL))
                .andRespond(
                        withSuccess(
                                "{invalido",
                                MediaType.APPLICATION_JSON
                        )
                );

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                assertThrows(
                        EnderecoException.class,
                        () -> client.consultar("01001000")
                ).getStatus()
        );

        server.verify();
    }

    @Test
    void respostaSemCidadeRetorna502() {
        server.expect(requestTo(URL))
                .andRespond(
                        withSuccess(
                                "{\"cep\":\"01001-000\",\"uf\":\"SP\"}",
                                MediaType.APPLICATION_JSON
                        )
                );

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                assertThrows(
                        EnderecoException.class,
                        () -> client.consultar("01001000")
                ).getStatus()
        );

        server.verify();
    }

    @Test
    void cepGenericoExigeComplementacaoManual() {
        server.expect(requestTo(URL))
                .andRespond(
                        withSuccess(
                                RESPOSTA.replace("Praça da Sé", ""),
                                MediaType.APPLICATION_JSON
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(
                        EnderecoException.class,
                        () -> service.resolver(
                                new EnderecoRequestDto(
                                        "01001000",
                                        null,
                                        "100",
                                        null,
                                        null,
                                        null,
                                        null
                                )
                        )
                ).getStatus()
        );

        server.verify();
    }

    @Test
    void cepGenericoAceitaLogradouroManual() {
        server.expect(requestTo(URL))
                .andRespond(
                        withSuccess(
                                RESPOSTA.replace("Praça da Sé", ""),
                                MediaType.APPLICATION_JSON
                        )
                );

        var endereco = service.resolver(
                new EnderecoRequestDto(
                        "01001000",
                        "Rua Informada",
                        "100",
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "Rua Informada",
                endereco.getLogradouro()
        );

        assertNull(endereco.getComplemento());
        server.verify();
    }

    @Test
    void divergenciaNaoESobrescritaSilenciosamente() {
        server.expect(requestTo(URL))
                .andRespond(
                        withSuccess(RESPOSTA, MediaType.APPLICATION_JSON)
                );

        assertEquals(
                HttpStatus.UNPROCESSABLE_ENTITY,
                assertThrows(
                        EnderecoException.class,
                        () -> service.resolver(
                                new EnderecoRequestDto(
                                        "01001000",
                                        "Outra Rua",
                                        "100",
                                        null,
                                        null,
                                        null,
                                        null
                                )
                        )
                ).getStatus()
        );

        server.verify();
    }

    @Test
    void comparacaoToleraCapitalizacaoEAcentos() {
        server.expect(requestTo(URL))
                .andRespond(
                        withSuccess(RESPOSTA, MediaType.APPLICATION_JSON)
                );

        var endereco = service.resolver(
                new EnderecoRequestDto(
                        "01001000",
                        "praca da se",
                        "100",
                        null,
                        "se",
                        "sao paulo",
                        "sp"
                )
        );

        assertEquals(
                "Praça da Sé",
                endereco.getLogradouro()
        );

        server.verify();
    }
}