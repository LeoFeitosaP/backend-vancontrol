package com.VanControl.VanControl.grafo;

import com.VanControl.VanControl.grafo.repository.GeografiaRepository;
import com.VanControl.VanControl.grafo.service.EmparelhamentoService;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.veiculo.domain.enums.StatusEnum;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagem.service.RegrasReserva;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmparelhamentoDistanciaTest {

    private final GeografiaRepository geografia =
            mock(GeografiaRepository.class);

    private final ViagemPassageiroRepository associacoes =
            mock(ViagemPassageiroRepository.class);

    private final EmparelhamentoService service =
            new EmparelhamentoService(geografia, associacoes);

    private final Passageiro passageiro =
            Passageiro.builder().id(UUID.randomUUID()).build();

    @Test
    void distanciaAusenteNaoViraZeroNemConsultaProvedor() {
        var conhecida = viagem("VIA-B");
        var desconhecida = viagem("VIA-A");

        when(geografia.distanciasRodoviarias(passageiro.getId()))
                .thenReturn(Map.of(conhecida.getId(), 11.0));

        var resultado = service.recomendarViagensParaPassageiro(
                passageiro,
                List.of(desconhecida, conhecida)
        );

        assertSame(conhecida, resultado.getFirst().viagem());
        assertEquals(1.0, resultado.getFirst().peso());
        assertEquals(Double.MAX_VALUE, resultado.getLast().peso());

        verify(geografia).distanciasRodoviarias(passageiro.getId());
        verifyNoMoreInteractions(geografia);
    }

    @Test
    void excluiInelegiveisAntesDeConsultarDistancias() {
        var lotada = viagem("VIA-LOTADA");

        var vencida = viagem("VIA-VENCIDA");
        vencida.setDataViagem(LocalDate.now(RegrasReserva.FUSO));

        var concluida = viagem("VIA-CONCLUIDA");
        concluida.setViagemConcluida(true);

        var manutencao = viagem("VIA-MANUTENCAO");
        manutencao.getVeiculo().setStatus(StatusEnum.MANUTENCAO);

        var associada = viagem("VIA-ASSOCIADA");

        when(associacoes.countByViagem_Id(lotada.getId()))
                .thenReturn(10L);

        when(associacoes.existsByViagem_IdAndPassageiro_Id(
                associada.getId(),
                passageiro.getId()
        )).thenReturn(true);

        var resultado = service.recomendarViagensParaPassageiro(
                passageiro,
                List.of(
                        lotada,
                        vencida,
                        concluida,
                        manutencao,
                        associada
                )
        );

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(geografia);
    }

    @Test
    void desempataDeFormaEstavelEPenalizaDistanciaInvalida() {
        var a = viagem("VIA-A");
        var b = viagem("VIA-B");
        var c = viagem("VIA-C");

        when(geografia.distanciasRodoviarias(passageiro.getId()))
                .thenReturn(
                        Map.of(
                                a.getId(), 11.0,
                                b.getId(), 11.0,
                                c.getId(), Double.NaN
                        )
                );

        var resultado = service.recomendarViagensParaPassageiro(
                passageiro,
                List.of(c, b, a)
        );

        assertEquals(
                List.of(a, b, c),
                resultado.stream().map(r -> r.viagem()).toList()
        );

        assertEquals(Double.MAX_VALUE, resultado.getLast().peso());
    }

    private Viagem viagem(String codigo) {
        return Viagem.builder()
                .id(UUID.randomUUID())
                .codigoViagem(codigo)
                .dataViagem(LocalDate.now(RegrasReserva.FUSO).plusDays(5))
                .horarioSaidaPrevisto(LocalTime.of(18, 0))
                .horarioChegadaPrevisto(LocalTime.of(19, 0))
                .veiculo(
                        Veiculo.builder()
                                .capacidade(10)
                                .status(StatusEnum.DISPONIVEL)
                                .build()
                )
                .build();
    }
}