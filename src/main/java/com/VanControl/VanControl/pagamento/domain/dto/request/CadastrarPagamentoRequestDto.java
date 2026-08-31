package com.VanControl.VanControl.pagamento.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CadastrarPagamentoRequestDto(

        @NotBlank(message = "O CPF não pode estar em branco")
        @CPF(message = "O formato do CPF é inválido")
        String cpf,

        @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{4}$", message = "Insira a competência no formato (mês/ano)")
        @NotNull(message = "Insira a competencia do pagamento")
        String competencia,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.1", message = "O valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "A data de vencimento é obrigatório, insira nesse formato (yyyy-MM-dd)")
        LocalDate dataVencimento
) {
}
