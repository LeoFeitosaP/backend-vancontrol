package com.VanControl.VanControl.grafo;

import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Ponto;
import com.VanControl.VanControl.grafo.service.NominatimClient;
import com.VanControl.VanControl.grafo.service.OsrmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.EstadoGeocodificacao.*;
import static com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.MetodoDistancia.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ClientesGeograficosTest {

    private MockRestServiceServer server;
    private NominatimClient nominatim;
    private OsrmClient osrm;

    private final Ponto origem = new Ponto(-23.5505, -46.6333);
    private final Ponto destino = new Ponto(-23.56, -46.64);

    private static final String CANDIDATO = """
            {"lat":"-23.5505","lon":"-46.6333","address":{
              "road":"Praça da Sé","house_number":"100","city":"São Paulo",
              "ISO3166-2-lvl4":"BR-SP","postcode":"01001-000","country_code":"br"}}
            """;

    private static final String ROTA = """
            {"code":"Ok","routes":[{"distance":12500}],
             "waypoints":[{"distance":3},{"distance":4}]}
            """;

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder()
                .baseUrl("https://geo.test")
                .defaultHeader("User-Agent", "VanControl-Test");

        server = MockRestServiceServer.bindTo(builder).build();

        var client = builder.build();

        nominatim = new NominatimClient(client);
        osrm = new OsrmClient(client, 100);
    }

    @Test
    void enviaEnderecoCompletoSemComplementoEConfirmaNumero() {
        server.expect(request -> {
                    var uri = request.getURI();

                    assertEquals("/search", uri.getPath());

                    String query = URLDecoder.decode(
                            uri.getRawQuery(),
                            StandardCharsets.UTF_8
                    );

                    assertTrue(query.contains(
                            "Praça da Sé, 100, Sé, São Paulo, SP, 01001000, Brasil"
                    ));

                    assertTrue(query.contains("limit=5"));
                    assertTrue(query.contains("addressdetails=1"));
                    assertFalse(query.contains("Apto"));

                }).andExpect(header("User-Agent", "VanControl-Test"))
                .andRespond(withSuccess(
                        "[" + CANDIDATO + "]",
                        MediaType.APPLICATION_JSON
                ));

        var resultado = nominatim.consultar(endereco());

        assertEquals(CONFIRMADA, resultado.estado());
        assertEquals(origem, resultado.coordenadas());

        server.verify();
    }

    @Test
    void candidatosEmPontosDiferentesSaoAmbiguos() {
        String outro = CANDIDATO.replace("-23.5505", "-23.5605");

        responder("[" + CANDIDATO + "," + outro + "]");

        var resultado = nominatim.consultar(endereco());

        assertEquals(AMBIGUA, resultado.estado());
        assertNull(resultado.coordenadas());
    }

    @Test
    void candidatosDuplicadosNaoCriamAmbiguidadeArtificial() {
        responder("[" + CANDIDATO + "," + CANDIDATO + "]");

        assertEquals(
                CONFIRMADA,
                nominatim.consultar(endereco()).estado()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "house_number",
            "road",
            "city",
            "ISO3166-2-lvl4",
            "postcode"
    })
    void dadosInsuficientesNaoConfirmamEndereco(String campo) {
        responder(
                "[" + CANDIDATO.replace(
                        "\"" + campo + "\"",
                        "\"ausente\""
                ) + "]"
        );

        assertEquals(
                IMPRECISA,
                nominatim.consultar(endereco()).estado()
        );
    }

    @Test
    void nenhumCandidatoDistingueEnderecoNaoEncontrado() {
        responder("[]");

        assertEquals(
                NAO_ENCONTRADA,
                nominatim.consultar(endereco()).estado()
        );
    }

    @Test
    void latitudeInvalidaNaoEPersistivel() {
        responder(
                "[" + CANDIDATO.replace("-23.5505", "NaN") + "]"
        );

        assertEquals(
                RESPOSTA_INVALIDA,
                nominatim.consultar(endereco()).estado()
        );
    }

    @Test
    void jsonMalformadoRetornaRespostaInvalida() {
        responder("{invalido");

        assertEquals(
                RESPOSTA_INVALIDA,
                nominatim.consultar(endereco()).estado()
        );
    }

    @Test
    void timeoutNaoSignificaEnderecoInexistente() {
        server.expect(anything())
                .andRespond(withException(new SocketTimeoutException()));

        assertEquals(
                INDISPONIVEL,
                nominatim.consultar(endereco()).estado()
        );
    }

    @Test
    void limiteDoProvedorNaoSignificaEnderecoInexistente() {
        server.expect(anything())
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertEquals(
                INDISPONIVEL,
                nominatim.consultar(endereco()).estado()
        );
    }

    @Test
    void enderecoLegadoIncompletoNaoConsultaProvedor() {
        assertEquals(
                ENDERECO_INCOMPLETO,
                nominatim.consultar(null).estado()
        );

        server.verify();
    }

    @Test
    void servidorPublicoNaoRecebeEnderecoResidencial() {
        var client = new NominatimClient(
                "https://nominatim.openstreetmap.org",
                "VanControl",
                3000
        );

        assertEquals(
                NAO_CONFIGURADO,
                client.consultar(endereco()).estado()
        );
    }

    @Test
    void respeitaSentidoLongitudeLatitudeEConverteMetrosEmKm() {
        server.expect(requestTo(
                        "https://geo.test/route/v1/driving/"
                                + "-46.6333000,-23.5505000;-46.6400000,-23.5600000"
                                + "?overview=false&alternatives=false&steps=false"
                ))
                .andRespond(withSuccess(
                        ROTA,
                        MediaType.APPLICATION_JSON
                ));

        var resultado = osrm.consultar(origem, destino, false);

        assertEquals(RODOVIARIA, resultado.metodo());
        assertEquals(12.5, resultado.distanciaKm());

        server.verify();
    }

    @Test
    void estimativaExigeOptInEInformaMotivo() {
        server.expect(anything())
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        var resultado = osrm.consultar(origem, destino, true);

        assertEquals(HAVERSINE, resultado.metodo());
        assertEquals("ESTIMADA", resultado.estado().name());
        assertEquals("INDISPONIVEL", resultado.motivo());

        assertTrue(
                resultado.distanciaKm() > 1
                        && resultado.distanciaKm() < 2
        );
    }

    @Test
    void falhaSemOptInNaoInventaDistancia() {
        server.expect(anything())
                .andRespond(withException(new SocketTimeoutException()));

        var resultado = osrm.consultar(origem, destino, false);

        assertEquals("INDISPONIVEL", resultado.estado().name());
        assertNull(resultado.metodo());
        assertNull(resultado.distanciaKm());
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 400})
    void ausenciaDeRotaNaoViraEstimativa(int status) {
        server.expect(anything())
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"NoRoute\"}"));

        var resultado = osrm.consultar(origem, destino, true);

        assertEquals("SEM_ROTA", resultado.estado().name());
        assertNull(resultado.distanciaKm());
    }

    @Test
    void ajusteDistanteDaViaNaoViraDistanciaRodoviaria() {
        responder(
                ROTA.replace("\"distance\":3", "\"distance\":1000")
        );

        assertEquals(
                "PONTO_FORA_DA_VIA",
                osrm.consultar(origem, destino, true).estado().name()
        );
    }

    @Test
    void rotaSemDistanciaNaoViraZero() {
        responder(
                ROTA.replace("{\"distance\":12500}", "{}")
        );

        var resultado = osrm.consultar(origem, destino, false);

        assertEquals("RESPOSTA_INVALIDA", resultado.estado().name());
        assertNull(resultado.distanciaKm());
    }

    @Test
    void estimativaAntipodalEFinita() {
        var client = new OsrmClient((RestClient) null, 100);

        var resultado = client.consultar(
                new Ponto(0.0, 0.0),
                new Ponto(0.0, 180.0),
                true
        );

        assertTrue(Double.isFinite(resultado.distanciaKm()));
        assertEquals(20015.1144, resultado.distanciaKm(), 0.01);
    }

    private void responder(String json) {
        server.expect(anything())
                .andRespond(withSuccess(
                        json,
                        MediaType.APPLICATION_JSON
                ));
    }

    private Endereco endereco() {
        return new Endereco(
                "01001000",
                "Praça da Sé",
                "100",
                "Apto 12",
                "Sé",
                "São Paulo",
                "SP"
        );
    }
}