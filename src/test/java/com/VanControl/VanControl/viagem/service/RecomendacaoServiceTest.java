package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.MetodoDistancia;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.rota.domain.entity.Rota;
import com.VanControl.VanControl.rota.repository.RotaRepository;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.viagem.domain.dto.request.ConsultarRecomendacoesRequestDto;
import com.VanControl.VanControl.viagem.domain.dto.response.RecomendacaoViagemResponseDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResponseDto;
import com.VanControl.VanControl.viagem.repository.RecomendacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecomendacaoServiceTest {

    @Mock
    private RecomendacaoRepository recomendacoes;

    @Mock
    private PassageiroRepository passageiros;

    @Mock
    private RotaRepository rotas;

    @Mock
    private SecurityUtils security;

    @InjectMocks
    private RecomendacaoService service;

    private final LocalDate dia =
            LocalDate.now(RegrasReserva.FUSO).plusDays(5);

    private final User usuario = new User();
    private final Passageiro passageiro = new Passageiro();
    private final Rota rota = new Rota();

    @Test
    void ordenaAntesDePaginarESomenteRodoviariaRecebePontuacao() {
        preparar();

        var estimada = candidata(
                "VIA-A", "ESTIMADA",
                MetodoDistancia.HAVERSINE, 0.1, 20
        );

        var distante = candidata(
                "VIA-D", "CALCULADA",
                MetodoDistancia.RODOVIARIA, 30.0, 1
        );

        var proxima = candidata(
                "VIA-P", "CALCULADA",
                MetodoDistancia.RODOVIARIA, 12.5, 1
        );

        when(recomendacoes.buscar(
                eq(passageiro.getId()),
                eq(rota.getId()),
                any(),
                any()
        )).thenReturn(List.of(estimada, distante, proxima));

        var primeira = service.buscar(filtro(), 0, 2);
        var segunda = service.buscar(filtro(), 1, 2);

        assertEquals(
                List.of("VIA-P", "VIA-D"),
                primeira.content().stream()
                        .map(r -> r.viagem().codigoViagem())
                        .toList()
        );

        assertEquals(6.25, primeira.content().getFirst().pontuacao());
        assertEquals(3, primeira.totalElements());
        assertEquals(2, primeira.totalPages());

        assertNull(segunda.content().getFirst().pontuacao());

        assertEquals(
                MetodoDistancia.HAVERSINE,
                segunda.content().getFirst().distancia().metodo()
        );

        verify(passageiros, times(2))
                .findByUser_Id(usuario.getId());
    }

    @Test
    void desempataPorCodigoEPaginaDistantePreservaTotal() {
        preparar();

        var b = candidata(
                "VIA-B", "NAO_CALCULADA_OU_EXPIRADA",
                null, null, 1
        );

        var a = candidata(
                "VIA-A", "NAO_CALCULADA_OU_EXPIRADA",
                null, null, 1
        );

        when(recomendacoes.buscar(any(), any(), any(), any()))
                .thenReturn(List.of(b, a));

        assertEquals(
                "VIA-A",
                service.buscar(filtro(), 0, 1)
                        .content().getFirst().viagem().codigoViagem()
        );

        var pagina = service.buscar(
                filtro(),
                Integer.MAX_VALUE,
                100
        );

        assertTrue(pagina.content().isEmpty());
        assertEquals(2, pagina.totalElements());
    }

    @Test
    void rejeitaPaginacaoAntesDeConsultarBanco() {
        assertThrows(
                BadRequestException.class,
                () -> service.buscar(filtro(), -1, 10)
        );

        assertThrows(
                BadRequestException.class,
                () -> service.buscar(filtro(), 0, 0)
        );

        assertThrows(
                BadRequestException.class,
                () -> service.buscar(filtro(), 0, 101)
        );

        verifyNoInteractions(
                recomendacoes,
                passageiros,
                rotas,
                security
        );
    }

    @Test
    void validaJanelaPrazoECamposObrigatorios() {
        LocalDate hoje = dia.minusDays(1);

        assertDoesNotThrow(() -> filtro().validar(hoje));

        assertThrows(
                BadRequestException.class,
                () -> filtro().validar(dia)
        );

        assertThrows(
                BadRequestException.class,
                () -> new ConsultarRecomendacoesRequestDto(
                        "ROT-TESTE",
                        dia,
                        dia.atTime(20, 0),
                        dia.atTime(19, 0)
                ).validar(hoje)
        );

        assertThrows(
                BadRequestException.class,
                () -> new ConsultarRecomendacoesRequestDto(
                        "ROT-TESTE",
                        dia,
                        dia.minusDays(1).atTime(19, 0),
                        dia.atTime(20, 0)
                ).validar(hoje)
        );

        assertThrows(
                BadRequestException.class,
                () -> new ConsultarRecomendacoesRequestDto(
                        "ROT-TESTE",
                        dia,
                        dia.atTime(19, 0),
                        dia.plusDays(2).atStartOfDay()
                ).validar(hoje)
        );

        assertThrows(
                BadRequestException.class,
                () -> new ConsultarRecomendacoesRequestDto(
                        null, null, null, null
                ).validar(hoje)
        );
    }

    @Test
    void passageiroAusenteRetorna404() {
        usuario.setId(UUID.randomUUID());

        when(security.getAuthenticatedUser())
                .thenReturn(usuario);

        when(passageiros.findByUser_Id(usuario.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.buscar(filtro(), 0, 10)
        );

        verifyNoInteractions(recomendacoes, rotas);
    }

    @Test
    void rotaSemDestinoNaoGeraRecomendacao() {
        preparar();
        rota.setDestino(" ");

        assertThrows(
                ConflictException.class,
                () -> service.buscar(filtro(), 0, 10)
        );

        verifyNoInteractions(recomendacoes);
    }

    private void preparar() {
        usuario.setId(UUID.randomUUID());
        passageiro.setId(UUID.randomUUID());
        rota.setId(UUID.randomUUID());
        rota.setDestino("UNISO");

        when(security.getAuthenticatedUser())
                .thenReturn(usuario);

        when(passageiros.findByUser_Id(usuario.getId()))
                .thenReturn(Optional.of(passageiro));

        when(rotas.findByCodigoRota("ROT-TESTE"))
                .thenReturn(Optional.of(rota));
    }

    private ConsultarRecomendacoesRequestDto filtro() {
        return new ConsultarRecomendacoesRequestDto(
                "ROT-TESTE",
                dia,
                dia.atTime(18, 30),
                dia.atTime(19, 30)
        );
    }

    private RecomendacaoViagemResponseDto candidata(
            String codigo,
            String estado,
            MetodoDistancia metodo,
            Double km,
            long vagas
    ) {
        return new RecomendacaoViagemResponseDto(
                new ViagemResponseDto(
                        codigo,
                        "ROT-TESTE",
                        "ABC-1D23",
                        "02245419006",
                        dia,
                        LocalTime.of(18, 0),
                        LocalTime.of(19, 0),
                        false
                ),
                "UNISO",
                dia.atTime(19, 0),
                vagas,
                new RecomendacaoViagemResponseDto.Distancia(
                        estado,
                        metodo,
                        km,
                        "CONFIRMADA",
                        "CONFIRMADA",
                        null,
                        null,
                        null
                ),
                null
        );
    }
}