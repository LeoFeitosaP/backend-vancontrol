package com.VanControl.VanControl.viagem.domain.dto.response;

import java.util.List;

public record PaginaRecomendacoesResponseDto(
        List<RecomendacaoViagemResponseDto> content,
        int number,
        int size,
        long totalElements,
        int totalPages
) {

    public PaginaRecomendacoesResponseDto {
        content = List.copyOf(content);
    }
}