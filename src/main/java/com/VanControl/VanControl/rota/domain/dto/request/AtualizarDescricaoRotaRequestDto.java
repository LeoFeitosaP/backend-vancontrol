package com.VanControl.VanControl.rota.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AtualizarDescricaoRotaRequestDto(
        @NotBlank(message = "A nova descrição não pode estar em branco")
        String novaDescricao
) {

}
