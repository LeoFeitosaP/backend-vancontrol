package com.VanControl.VanControl.grafo;

import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.*;
import com.VanControl.VanControl.grafo.domain.dto.GeografiaPersistida.*;
import com.VanControl.VanControl.grafo.repository.GeografiaRepository;
import com.VanControl.VanControl.grafo.service.GeoLocalizacaoService;
import com.VanControl.VanControl.grafo.service.NominatimClient;
import com.VanControl.VanControl.grafo.service.OsrmClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(
        named = "GEO_TEST_JDBC_URL",
        matches = ".+"
)
class GeografiaPostgresTest {

    private JdbcTemplate raiz;
    private JdbcTemplate jdbc;
    private String schema;

    private GeografiaRepository repository;
    private GeoLocalizacaoService service;
    private NominatimClient nominatim;
    private OsrmClient osrm;

    private static final String CPF = "52998224725";
    private static final String PLACA = "VAN-1A23";

    @BeforeEach
    void preparar() throws Exception {
        String url = System.getenv("GEO_TEST_JDBC_URL");

        String usuario = System.getenv().getOrDefault(
                "GEO_TEST_USER",
                "postgres"
        );

        String senha = System.getenv().getOrDefault(
                "GEO_TEST_PASSWORD",
                ""
        );

        raiz = new JdbcTemplate(
                new DriverManagerDataSource(url, usuario, senha)
        );

        schema = "geo_test_" + UUID.randomUUID()
                .toString()
                .replace("-", "");

        raiz.execute("create schema " + schema);

        var ds = new DriverManagerDataSource(url, usuario, senha) {
            @Override
            public Connection getConnection() throws SQLException {
                Connection connection = super.getConnection();

                try (var statement = connection.createStatement()) {
                    statement.execute("set search_path to " + schema);
                }

                return connection;
            }
        };

        jdbc = new JdbcTemplate(ds);

        jdbc.execute(
                Files.readString(
                        Path.of("src/test/resources/sql/geografia_fixture.sql")
                )
        );

        jdbc.execute(
                Files.readString(
                        Path.of(
                                "src/main/resources/db/migration/V006__geografia_versionada.sql"
                        )
                )
        );

        repository = new GeografiaRepository(
                jdbc,
                new DataSourceTransactionManager(ds)
        );

        nominatim = mock(NominatimClient.class);
        osrm = mock(OsrmClient.class);

        when(nominatim.consultar(any())).thenReturn(confirmada());

        when(osrm.consultar(any(), any(), anyBoolean()))
                .thenReturn(rodoviaria());

        service = new GeoLocalizacaoService(
                repository,
                nominatim,
                osrm,
                mock(EnderecoService.class)
        );
    }

    @AfterEach
    void limpar() {
        if (raiz != null && schema != null) {
            raiz.execute("drop schema " + schema + " cascade");
        }
    }

    @Test
    void migracaoPreservaLegadoSemInventarOrigemOuPrecisao() {
        var passageiro = service.consultar(Tipo.PASSAGEIRO, CPF);
        var viagem = service.consultar(Tipo.VIAGEM, "ANTIGA");

        assertEquals("PENDENTE", passageiro.estado());
        assertNull(passageiro.coordenadas());

        assertEquals("REVISAO_NECESSARIA", viagem.origemEndereco());
        assertEquals("ENDERECO_INCOMPLETO", viagem.estado());

        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from geo_distancia_legada",
                        Integer.class
                )
        );

        assertEquals(
                2,
                jdbc.queryForObject(
                        "select count(*) from geo_coordenada_legada",
                        Integer.class
                )
        );

        assertEquals(
                0,
                jdbc.queryForObject(
                        "select count(*) from passageiro_veiculo_distancia",
                        Integer.class
                )
        );
    }

    @Test
    void novaViagemCopiaBaseConfirmadaEBasePosteriorNaoAlteraHistorico() {
        service.processar(Tipo.VEICULO, PLACA, false);
        novaViagem();

        var antes = service.consultar(Tipo.VIAGEM, "NOVA");

        jdbc.update(
                "update veiculo set endereco_numero='200' where placa=?",
                PLACA
        );

        var depois = service.consultar(Tipo.VIAGEM, "NOVA");

        assertEquals(antes, depois);
        assertEquals("CONFIRMADA", depois.estado());
        assertEquals("100", depois.endereco().getNumero());

        assertEquals(
                "PENDENTE",
                service.consultar(Tipo.VEICULO, PLACA).estado()
        );
    }

    @Test
    void falhaEPersistidaESoReconsultaQuandoSolicitado() {
        when(nominatim.consultar(any())).thenReturn(
                new Geocodificacao(
                        EstadoGeocodificacao.INDISPONIVEL,
                        null,
                        "Falha",
                        "OSM"
                )
        );

        var primeira = service.processar(
                Tipo.PASSAGEIRO,
                CPF,
                false
        );

        var segunda = service.processar(
                Tipo.PASSAGEIRO,
                CPF,
                false
        );

        assertEquals(primeira, segunda);
        assertEquals("INDISPONIVEL", primeira.estado());
        assertNotNull(primeira.consultadoEm());

        verify(nominatim, times(1)).consultar(any());

        service.processar(Tipo.PASSAGEIRO, CPF, true);

        verify(nominatim, times(2)).consultar(any());
    }

    @Test
    void respostaAtrasadaNaoSobreviveAMudancaDeEnderecoMesmoVoltandoAoOriginal()
            throws Exception {
        var entrou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);

        when(nominatim.consultar(any())).thenAnswer(invocation -> {
            entrou.countDown();

            assertTrue(
                    liberar.await(10, TimeUnit.SECONDS)
            );

            return confirmada();
        });

        try (var executor = Executors.newSingleThreadExecutor()) {
            var futuro = executor.submit(
                    () -> service.processar(
                            Tipo.PASSAGEIRO,
                            CPF,
                            false
                    )
            );

            try {
                assertTrue(entrou.await(10, TimeUnit.SECONDS));

                jdbc.update(
                        "update passageiro set endereco_numero='200'"
                );

                jdbc.update(
                        "update passageiro set endereco_numero='100'"
                );

            } finally {
                liberar.countDown();
            }

            var erro = assertThrows(
                    ExecutionException.class,
                    () -> futuro.get(10, TimeUnit.SECONDS)
            );

            assertInstanceOf(
                    ConflictException.class,
                    erro.getCause()
            );
        }

        assertNull(
                service.consultar(Tipo.PASSAGEIRO, CPF).coordenadas()
        );
    }

    @Test
    void distanciaUsaRevisoesEAlteracaoDoPassageiroInvalidaCache() {
        prepararPontos();

        var calculo = service.distancia(
                "NOVA",
                CPF,
                false,
                true,
                false
        );

        assertEquals(
                MetodoDistancia.RODOVIARIA,
                calculo.resultado().metodo()
        );

        assertEquals(
                calculo,
                service.distancia(
                        "NOVA",
                        CPF,
                        false,
                        false,
                        false
                )
        );

        jdbc.update(
                "update passageiro set endereco_numero='200'"
        );

        assertEquals(
                0,
                jdbc.queryForObject(
                        "select count(*) from geo_distancia",
                        Integer.class
                )
        );

        assertThrows(
                ConflictException.class,
                () -> service.distancia(
                        "NOVA",
                        CPF,
                        false,
                        false,
                        false
                )
        );
    }

    @Test
    void distanciaAtrasadaNaoSalvaDepoisDeNovaGeocodificacao()
            throws Exception {
        prepararPontos();

        var entrou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);

        when(osrm.consultar(any(), any(), anyBoolean()))
                .thenAnswer(invocation -> {
                    entrou.countDown();

                    assertTrue(
                            liberar.await(10, TimeUnit.SECONDS)
                    );

                    return rodoviaria();
                });

        try (var executor = Executors.newSingleThreadExecutor()) {
            var futuro = executor.submit(
                    () -> service.distancia(
                            "NOVA",
                            CPF,
                            false,
                            true,
                            false
                    )
            );

            try {
                assertTrue(entrou.await(10, TimeUnit.SECONDS));

                service.processar(
                        Tipo.PASSAGEIRO,
                        CPF,
                        true
                );

            } finally {
                liberar.countDown();
            }

            var erro = assertThrows(
                    ExecutionException.class,
                    () -> futuro.get(10, TimeUnit.SECONDS)
            );

            assertInstanceOf(
                    ConflictException.class,
                    erro.getCause()
            );
        }

        assertEquals(
                0,
                jdbc.queryForObject(
                        "select count(*) from geo_distancia",
                        Integer.class
                )
        );
    }

    @Test
    void estimativaNaoViraDistanciaRodoviariaNoRanking() {
        prepararPontos();

        when(osrm.consultar(any(), any(), anyBoolean()))
                .thenReturn(
                        new Distancia(
                                EstadoDistancia.ESTIMADA,
                                MetodoDistancia.HAVERSINE,
                                1.0,
                                "INDISPONIVEL",
                                "Linha reta"
                        )
                );

        service.distancia(
                "NOVA",
                CPF,
                true,
                true,
                false
        );

        var id = jdbc.queryForObject(
                "select id from passageiro",
                UUID.class
        );

        assertTrue(
                repository.distanciasRodoviarias(id).isEmpty()
        );

        assertThrows(
                ConflictException.class,
                () -> service.distancia(
                        "NOVA",
                        CPF,
                        false,
                        false,
                        false
                )
        );
    }

    @Test
    void origemLegadaExigeRevisaoEOrigemConhecidaConcluidaNaoMuda() {
        Endereco e = new Endereco(
                "01001000",
                "Praça da Sé",
                "100",
                null,
                "Sé",
                "São Paulo",
                "SP"
        );

        repository.revisarOrigem(
                "ANTIGA",
                e,
                "Conferido no registro da viagem"
        );

        assertEquals(
                "REVISAO_MANUAL",
                service.consultar(
                        Tipo.VIAGEM,
                        "ANTIGA"
                ).origemEndereco()
        );

        jdbc.update(
                "update viagem set viagem_concluida=true "
                        + "where codigo_viagem='ANTIGA'"
        );

        assertThrows(
                ConflictException.class,
                () -> repository.revisarOrigem(
                        "ANTIGA",
                        e,
                        "Outra origem"
                )
        );
    }

    private void novaViagem() {
        jdbc.update("""
                insert into viagem (
                    codigo_viagem,
                    veiculo_id,
                    data_viagem,
                    horario_saida_previsto
                )
                select
                    'NOVA',
                    id_veiculo,
                    '2099-10-10',
                    '18:00'
                from veiculo
                where placa=?
                """,
                PLACA
        );
    }

    private void prepararPontos() {
        service.processar(Tipo.VEICULO, PLACA, false);
        novaViagem();
        service.processar(Tipo.PASSAGEIRO, CPF, false);
    }

    private Geocodificacao confirmada() {
        return new Geocodificacao(
                EstadoGeocodificacao.CONFIRMADA,
                new Ponto(-23.55, -46.63),
                "Confirmado",
                "OSM"
        );
    }

    private Distancia rodoviaria() {
        return new Distancia(
                EstadoDistancia.CALCULADA,
                MetodoDistancia.RODOVIARIA,
                12.5,
                null,
                "OSRM"
        );
    }
}