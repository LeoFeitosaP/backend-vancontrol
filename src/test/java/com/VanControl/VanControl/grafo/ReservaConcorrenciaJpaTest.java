package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.grafo.service.EmparelhamentoService;
import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.rota.domain.entity.Rota;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.veiculo.domain.enums.StatusEnum;
import com.VanControl.VanControl.veiculo.repository.VeiculoRepository;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ViagemService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservaConcorrenciaJpaTest {

    private static final String CODIGO = "VIA-CONCORRENCIA";
    private static final String CPF_A = "52998224725";
    private static final String CPF_B = "11144477735";

    private static final String SCHEMA =
            "reserva_test_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired
    private ViagemService service;

    @Autowired
    private ViagemPassageiroRepository associacoes;

    @Autowired
    private VeiculoRepository veiculos;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager manager;

    @MockitoBean
    private EmparelhamentoService emparelhamento;

    @MockitoBean
    private SecurityUtils security;

    private UUID viagemId;
    private UUID veiculoId;
    private TransactionTemplate transacao;

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry properties) throws Exception {
        String url = System.getenv("RESERVA_TEST_JDBC_URL");

        properties.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );

        properties.add(
                "spring.datasource.hikari.maximum-pool-size",
                () -> 5
        );

        if (url == null || url.isBlank()) {
            properties.add(
                    "spring.datasource.url",
                    () -> "jdbc:h2:mem:reserva_concorrencia;"
                            + "DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"
            );

            properties.add(
                    "spring.datasource.driver-class-name",
                    () -> "org.h2.Driver"
            );

            properties.add("spring.datasource.username", () -> "sa");
            properties.add("spring.datasource.password", () -> "");

            properties.add(
                    "spring.jpa.database-platform",
                    () -> "org.hibernate.dialect.H2Dialect"
            );

            return;
        }

        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException(
                    "RESERVA_TEST_JDBC_URL deve apontar para PostgreSQL de testes"
            );
        }

        String usuario = System.getenv()
                .getOrDefault("RESERVA_TEST_USER", "postgres");

        String senha = System.getenv()
                .getOrDefault("RESERVA_TEST_PASSWORD", "");

        try (
                var connection = DriverManager.getConnection(url, usuario, senha);
                var statement = connection.createStatement()
        ) {
            statement.execute("create schema " + SCHEMA);
        }

        properties.add("spring.datasource.url", () -> url);

        properties.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );

        properties.add("spring.datasource.username", () -> usuario);
        properties.add("spring.datasource.password", () -> senha);

        properties.add(
                "spring.datasource.hikari.connection-init-sql",
                () -> "set search_path to " + SCHEMA
        );

        properties.add(
                "spring.jpa.properties.hibernate.default_schema",
                () -> SCHEMA
        );

        properties.add(
                "spring.jpa.database-platform",
                () -> "org.hibernate.dialect.PostgreSQLDialect"
        );
    }

    @BeforeEach
    void preparar() {
        transacao = new TransactionTemplate(manager);

        transacao.executeWithoutResult(status -> {
            for (String tabela : new String[]{
                    "viagem_passageiro",
                    "viagem",
                    "motorista",
                    "passageiro",
                    "veiculo",
                    "rota",
                    "users"
            }) {
                jdbc.update("delete from " + tabela);
            }

            var rota = new Rota();
            rota.setCodigoRota("ROT-TESTE");
            em.persist(rota);

            var veiculo = new Veiculo(
                    "ABC-1D23",
                    "Mercedes",
                    "Sprinter",
                    2024,
                    1,
                    "12345678901",
                    "DISPONIVEL"
            );

            em.persist(veiculo);
            veiculoId = veiculo.getIdVeiculo();

            var motorista = new Motorista();
            motorista.setUser(usuario("02245419006", Role.MOTORISTA));
            em.persist(motorista);

            for (String cpf : new String[]{CPF_A, CPF_B}) {
                var passageiro = new Passageiro();
                passageiro.setUser(usuario(cpf, Role.PASSAGEIRO));
                em.persist(passageiro);
            }

            var viagem = new Viagem(
                    rota,
                    veiculo,
                    motorista,
                    LocalDate.now(RegrasReserva.FUSO).plusDays(5),
                    LocalTime.of(18, 0),
                    LocalTime.of(19, 0)
            );

            viagem.setCodigoViagem(CODIGO);
            em.persist(viagem);
            em.flush();

            viagemId = viagem.getId();
        });
    }

    @Test
    void duasRequisicoesNaoOcupamUltimaVaga() throws Exception {
        String resultado = disputar(
                () -> service.adicionarPassageiro(CODIGO, CPF_A),
                CPF_B
        );

        assertEquals(
                "400:Capacidade máxima do veículo atingida",
                resultado
        );

        assertEquals(1, associacoes.countByViagem_Id(viagemId));
    }

    @Test
    void repeticaoConcorrenteNaoDuplicaPassageiro() throws Exception {
        String resultado = disputar(
                () -> service.adicionarPassageiro(CODIGO, CPF_A),
                CPF_A
        );

        assertEquals(
                "409:Passageiro já está associado a esta viagem",
                resultado
        );

        assertEquals(1, associacoes.countByViagem_Id(viagemId));
    }

    @Test
    void conclusaoConfirmadaAntesDaReservaImpedeAssociacao() throws Exception {
        String resultado = disputar(
                () -> service.atualizarStatusViagem(CODIGO),
                CPF_A
        );

        assertEquals(
                "400:Viagem concluída não permite novas associações",
                resultado
        );

        assertEquals(0, associacoes.countByViagem_Id(viagemId));
    }

    @Test
    void remocaoConfirmadaLiberaVagaParaProximaReserva() throws Exception {
        service.adicionarPassageiro(CODIGO, CPF_A);

        String resultado = disputar(
                () -> service.removerPassageiro(CODIGO, CPF_A),
                CPF_B
        );

        assertEquals("200", resultado);
        assertEquals(1, associacoes.countByViagem_Id(viagemId));
    }

    @Test
    void alteracaoConcorrenteDoVeiculoEhObservadaAposBloqueio() throws Exception {
        String resultado = disputar(() -> {
            var veiculo = veiculos.buscarParaAtualizacao(veiculoId);
            veiculo.setStatus(StatusEnum.MANUTENCAO);
            veiculos.saveAndFlush(veiculo);
        }, CPF_A);

        assertEquals(
                "400:Veículo indisponível para novas reservas",
                resultado
        );

        assertEquals(0, associacoes.countByViagem_Id(viagemId));
    }

    private String disputar(
            Runnable primeiraOperacao,
            String segundoCpf
    ) throws Exception {
        var primeiraExecutada = new CountDownLatch(1);
        var segundaIniciada = new CountDownLatch(1);
        var permitirCommit = new CountDownLatch(1);

        var executor = Executors.newFixedThreadPool(2);

        try {
            var primeira = executor.submit(
                    () -> transacao.executeWithoutResult(status -> {
                        primeiraOperacao.run();
                        em.flush();
                        primeiraExecutada.countDown();
                        aguardar(permitirCommit);
                    })
            );

            assertTrue(
                    primeiraExecutada.await(10, TimeUnit.SECONDS)
            );

            var segunda = executor.submit(() -> {
                segundaIniciada.countDown();

                try {
                    service.adicionarPassageiro(CODIGO, segundoCpf);
                    return "200";
                } catch (BadRequestException ex) {
                    return "400:" + ex.getMessage();
                } catch (ConflictException ex) {
                    return "409:" + ex.getMessage();
                }
            });

            assertTrue(
                    segundaIniciada.await(10, TimeUnit.SECONDS)
            );

            assertThrows(
                    TimeoutException.class,
                    () -> segunda.get(300, TimeUnit.MILLISECONDS)
            );

            permitirCommit.countDown();

            primeira.get(10, TimeUnit.SECONDS);
            return segunda.get(10, TimeUnit.SECONDS);

        } finally {
            permitirCommit.countDown();
            executor.shutdown();

            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                fail("Transações de teste não terminaram");
            }
        }
    }

    private User usuario(String cpf, Role role) {
        var user = new User();
        user.setCpf(cpf);
        user.setEmail(cpf + "@example.com");
        user.setRole(role);
        em.persist(user);
        return user;
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Tempo de coordenação excedido"
                );
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}