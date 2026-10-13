package com.VanControl.VanControl.grafo;

import com.VanControl.VanControl.grafo.repository.GeografiaRepository;
import com.VanControl.VanControl.grafo.service.EmparelhamentoService;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmparelhamentoDistanciaTest {

    @Test
    void distanciaAusenteNaoViraZeroNemConsultaProvedor() {
        var repository = mock(GeografiaRepository.class);

        var passageiros = mock(
                ViagemPassageiroRepository.class
        );

        var service = new EmparelhamentoService(
                repository,
                passageiros
        );

        var passageiro = Passageiro.builder()
                .id(UUID.randomUUID())
                .build();

        var veiculo = Veiculo.builder()
                .capacidade(10)
                .build();

        var conhecida = Viagem.builder()
                .id(UUID.randomUUID())
                .codigoViagem("VIA-B")
                .veiculo(veiculo)
                .build();

        var desconhecida = Viagem.builder()
                .id(UUID.randomUUID())
                .codigoViagem("VIA-A")
                .veiculo(veiculo)
                .build();

        when(
                repository.distanciasRodoviarias(
                        passageiro.getId()
                )
        ).thenReturn(
                Map.of(conhecida.getId(), 11.0)
        );

        var resultado = service.recomendarViagensParaPassageiro(
                passageiro,
                List.of(desconhecida, conhecida)
        );

        assertSame(
                conhecida,
                resultado.getFirst().viagem()
        );

        assertEquals(
                1.0,
                resultado.getFirst().peso()
        );

        assertEquals(
                Double.MAX_VALUE,
                resultado.getLast().peso()
        );

        verify(repository).distanciasRodoviarias(
                passageiro.getId()
        );

        verifyNoMoreInteractions(repository);
    }
}