package com.VanControl.VanControl.motorista.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.YearMonth;

public record MotoristaResponseDto(
            String nome,
            String cpf,
            String email,
            String cnh,
            String categoriaCnh,
            @JsonFormat(pattern = "MM/yyyy")
            YearMonth dataValidadeCnh,
            String telefone
) {
}
