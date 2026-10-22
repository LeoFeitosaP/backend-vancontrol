package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.veiculo.domain.enums.StatusEnum;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class RegrasReservaTest {

    private static final LocalDate DIA = LocalDate.of(2099, 10, 10);

    @Test
    void aceitaAteDiaAnteriorMasRecusaDiaDaViagemEPassado() {
        var viagem = viagem();

        assertNull(
                RegrasReserva.impedimento(viagem, 0, DIA.minusDays(1))
        );

        assertNotNull(
                RegrasReserva.impedimento(viagem, 0, DIA)
        );

        assertNotNull(
                RegrasReserva.impedimento(viagem, 0, DIA.plusDays(1))
        );
    }

    @Test
    void viradaDoDiaUsaFusoDeSaoPaulo() {
        assertEquals(
                LocalDate.of(2026, 10, 12),
                Instant.parse("2026-10-13T02:59:59Z")
                        .atZone(RegrasReserva.FUSO)
                        .toLocalDate()
        );

        assertEquals(
                LocalDate.of(2026, 10, 13),
                Instant.parse("2026-10-13T03:00:00Z")
                        .atZone(RegrasReserva.FUSO)
                        .toLocalDate()
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = StatusEnum.class,
            names = {"MANUTENCAO", "INATIVO"}
    )
    void recusaVeiculoIndisponivel(StatusEnum status) {
        var viagem = viagem();
        viagem.getVeiculo().setStatus(status);

        assertEquals(
                "Veículo indisponível para novas reservas",
                RegrasReserva.impedimento(viagem, 0, DIA.minusDays(1))
        );
    }

    @Test
    void recusaLotacaoEConfiguracaoIncompleta() {
        var viagem = viagem();

        assertEquals(
                "Capacidade máxima do veículo atingida",
                RegrasReserva.impedimento(viagem, 2, DIA.minusDays(1))
        );

        viagem.getVeiculo().setCapacidade(0);

        assertNotNull(
                RegrasReserva.impedimento(viagem, 0, DIA.minusDays(1))
        );

        viagem.getVeiculo().setCapacidade(2);
        viagem.setHorarioSaidaPrevisto(null);

        assertNotNull(
                RegrasReserva.impedimento(viagem, 0, DIA.minusDays(1))
        );
    }

    @Test
    void recusaConcluida() {
        var viagem = viagem();
        viagem.setViagemConcluida(true);

        assertEquals(
                "Viagem concluída não permite novas associações",
                RegrasReserva.impedimento(viagem, 0, DIA.minusDays(1))
        );
    }

    private Viagem viagem() {
        return Viagem.builder()
                .dataViagem(DIA)
                .horarioSaidaPrevisto(LocalTime.of(18, 0))
                .horarioChegadaPrevisto(LocalTime.of(19, 0))
                .veiculo(
                        Veiculo.builder()
                                .capacidade(2)
                                .status(StatusEnum.DISPONIVEL)
                                .build()
                )
                .build();
    }
}