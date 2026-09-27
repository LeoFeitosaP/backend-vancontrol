package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.grafo.service.EmparelhamentoService;
import com.VanControl.VanControl.motorista.service.MotoristaService;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.rota.service.RotaService;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.veiculo.service.VeiculoService;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResumoResponseDto;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagemPassageiro.domain.entity.ViagemPassageiro;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(ViagemService.class)
class ViagensPassageiroJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ViagemService viagemService;

    @MockitoBean
    private EmparelhamentoService emparelhamentoService;

    @MockitoBean
    private RotaService rotaService;

    @MockitoBean
    private VeiculoService veiculoService;

    @MockitoBean
    private MotoristaService motoristaService;

    @MockitoBean
    private SecurityUtils securityUtils;

    private static final String CPF = "02245419006";

    @Test
    void ordenaNoBancoAntesDePaginarEFiltraPassageiro() {
        Passageiro passageiro = criarPassageiro(CPF);

        criarAssociacao(passageiro, "VIA-ANTIGA", 1, 18);
        criarAssociacao(passageiro, "VIA-NOVA", 3, 18);
        criarAssociacao(passageiro, "VIA-MEIO", 2, 18);

        criarAssociacao(
                criarPassageiro("52998224725"),
                "VIA-OUTRO",
                4,
                18
        );

        sincronizar();

        var primeira = viagemService.listarViagensPorPassageiroCpf(
                CPF,
                PageRequest.of(
                        0,
                        2,
                        Sort.by(Sort.Direction.DESC, "dataViagem")
                )
        );

        var segunda = viagemService.listarViagensPorPassageiroCpf(
                CPF,
                PageRequest.of(
                        1,
                        2,
                        Sort.by(Sort.Direction.DESC, "dataViagem")
                )
        );

        assertEquals(
                List.of("VIA-NOVA", "VIA-MEIO"),
                primeira.getContent().stream()
                        .map(ViagemResumoResponseDto::codigoViagem)
                        .toList()
        );

        assertEquals(
                List.of("VIA-ANTIGA"),
                segunda.getContent().stream()
                        .map(ViagemResumoResponseDto::codigoViagem)
                        .toList()
        );

        assertEquals(3, primeira.getTotalElements());
        assertEquals(3, segunda.getTotalElements());
        assertEquals(2, primeira.getTotalPages());
    }

    @Test
    void suportaOrdenacaoCrescenteComposta() {
        Passageiro passageiro = criarPassageiro(CPF);

        criarAssociacao(passageiro, "VIA-NOITE", 1, 19);
        criarAssociacao(passageiro, "VIA-TARDE", 1, 17);
        criarAssociacao(passageiro, "VIA-AMANHA", 2, 16);

        sincronizar();

        var pagina = viagemService.listarViagensPorPassageiroCpf(
                CPF,
                PageRequest.of(
                        0,
                        10,
                        Sort.by(
                                Sort.Order.asc("dataViagem"),
                                Sort.Order.asc("horarioSaidaPrevisto")
                        )
                )
        );

        assertEquals(
                List.of("VIA-TARDE", "VIA-NOITE", "VIA-AMANHA"),
                pagina.getContent().stream()
                        .map(ViagemResumoResponseDto::codigoViagem)
                        .toList()
        );
    }

    @Test
    void datasIguaisPossuemDesempateEstavelEntrePaginas() {
        Passageiro passageiro = criarPassageiro(CPF);

        criarAssociacao(passageiro, "VIA-A", 1, 18);
        criarAssociacao(passageiro, "VIA-B", 1, 18);
        criarAssociacao(passageiro, "VIA-C", 1, 18);

        sincronizar();

        List<String> primeiraLeitura = lerTresPaginas();

        entityManager.clear();

        List<String> segundaLeitura = lerTresPaginas();

        assertEquals(3, primeiraLeitura.stream().distinct().count());
        assertEquals(primeiraLeitura, segundaLeitura);
    }

    @Test
    void paginaForaDoIntervaloPreservaTotal() {
        criarAssociacao(
                criarPassageiro(CPF),
                "VIA-UNICA",
                1,
                18
        );

        sincronizar();

        var pagina = viagemService.listarViagensPorPassageiroCpf(
                CPF,
                PageRequest.of(5, 2, Sort.by("dataViagem"))
        );

        assertTrue(pagina.isEmpty());
        assertEquals(1, pagina.getTotalElements());
        assertEquals(5, pagina.getNumber());
    }

    @Test
    void passageiroSemViagensRecebePaginaVazia() {
        criarPassageiro(CPF);
        sincronizar();

        var pagina = viagemService.listarViagensPorPassageiroCpf(
                CPF,
                PageRequest.of(0, 10)
        );

        assertTrue(pagina.isEmpty());
        assertEquals(0, pagina.getTotalElements());
    }

    private List<String> lerTresPaginas() {
        return IntStream.range(0, 3)
                .mapToObj(numero -> viagemService.listarViagensPorPassageiroCpf(
                        CPF,
                        PageRequest.of(
                                numero,
                                1,
                                Sort.by("dataViagem")
                        )
                ).getContent().getFirst().codigoViagem())
                .toList();
    }

    private Passageiro criarPassageiro(String cpf) {
        var user = new User();
        user.setName("Passageiro Teste");
        user.setCpf(cpf);
        user.setEmail(cpf + "@example.com");
        user.setPassword("hash-apenas-para-teste");
        user.setRole(Role.PASSAGEIRO);

        entityManager.persist(user);

        var passageiro = new Passageiro();
        passageiro.setUser(user);

        entityManager.persist(passageiro);

        return passageiro;
    }

    private void criarAssociacao(
            Passageiro passageiro,
            String codigo,
            int dia,
            int hora
    ) {
        var viagem = new Viagem(
                "ROT-TESTE",
                "ABC-1D23",
                "52998224725",
                LocalDate.of(2099, 10, dia),
                LocalTime.of(hora, 0),
                LocalTime.of(hora + 1, 0)
        );

        viagem.setCodigoViagem(codigo);

        entityManager.persist(viagem);

        var associacao = ViagemPassageiro.builder()
                .viagem(viagem)
                .passageiro(passageiro)
                .dataAssociacao(LocalDateTime.of(2099, 9, 1, 12, 0))
                .build();

        entityManager.persist(associacao);
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }
}
