package com.VanControl.VanControl.pagamento.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AtualizarStatusPagamentoRequestDto(

        @NotBlank(message = " O status é obrigatório")
        String status,

        @NotNull(message = "Insira a data que foi realizado o pagamento nesse formato (yyyy-MM-dd)")
        LocalDate dataPagamento
) {
}
