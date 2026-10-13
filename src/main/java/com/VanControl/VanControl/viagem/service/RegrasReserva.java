package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.veiculo.domain.enums.StatusEnum;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;

import java.time.LocalDate;
import java.time.ZoneId;

public final class RegrasReserva {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private RegrasReserva() {
    }

    public static String impedimento(
            Viagem viagem,
            long ocupacao,
            LocalDate hoje
    ) {
        if (viagem.isViagemConcluida()) {
            return "Viagem concluída não permite novas associações";
        }

        if (viagem.getDataViagem() == null
                || viagem.getHorarioSaidaPrevisto() == null
                || viagem.getHorarioChegadaPrevisto() == null) {
            return "Viagem sem data ou horários definidos";
        }

        if (!viagem.getDataViagem().isAfter(hoje)) {
            return "Associar passageiro permitido apenas até um dia antes da viagem";
        }

        var veiculo = viagem.getVeiculo();

        if (veiculo == null || veiculo.getStatus() != StatusEnum.DISPONIVEL) {
            return "Veículo indisponível para novas reservas";
        }

        if (veiculo.getCapacidade() <= 0) {
            return "Veículo sem capacidade válida";
        }

        if (ocupacao >= veiculo.getCapacidade()) {
            return "Capacidade máxima do veículo atingida";
        }

        return null;
    }

    public static void validar(
            Viagem viagem,
            long ocupacao,
            LocalDate hoje
    ) {
        String motivo = impedimento(viagem, ocupacao, hoje);

        if (motivo != null) {
            throw new BadRequestException(motivo);
        }
    }
}