package com.VanControl.VanControl.viagem.domain.dto.request;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConsultarRecomendacoesRequestDto(
        @NotBlank
        @Size(max = 255)
        String codigoRota,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataViagem,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime chegadaDe,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime chegadaAte
) {

    public void validar(LocalDate hoje) {
        if (codigoRota == null
                || codigoRota.isBlank()
                || codigoRota.length() > 255
                || dataViagem == null
                || chegadaDe == null
                || chegadaAte == null) {
            throw new BadRequestException(
                    "Informe rota, data e janela de chegada válidas"
            );
        }

        if (!dataViagem.isAfter(hoje) || dataViagem.getYear() > 9998) {
            throw new BadRequestException(
                    "Informe uma data futura válida para a viagem"
            );
        }

        if (chegadaDe.isAfter(chegadaAte)) {
            throw new BadRequestException(
                    "chegadaDe deve ser anterior ou igual a chegadaAte"
            );
        }

        if (chegadaDe.isBefore(dataViagem.atStartOfDay())
                || !chegadaAte.isBefore(
                dataViagem.plusDays(2).atStartOfDay()
        )) {
            throw new BadRequestException(
                    "A chegada deve ocorrer no dia da saída ou no dia seguinte"
            );
        }
    }
}