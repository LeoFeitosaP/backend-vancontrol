package com.VanControl.VanControl.viagem.controller;

import com.VanControl.VanControl.viagem.domain.dto.request.ConsultarRecomendacoesRequestDto;
import com.VanControl.VanControl.viagem.domain.dto.response.PaginaRecomendacoesResponseDto;
import com.VanControl.VanControl.viagem.service.RecomendacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/viagens/recomendacoes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RecomendacaoController {

    private final RecomendacaoService service;

    @GetMapping
    @PreAuthorize("hasRole('PASSAGEIRO')")
    @Operation(
            summary = "Recomendar viagens por rota, data e janela de chegada"
    )
    public PaginaRecomendacoesResponseDto buscar(
            @Valid @ModelAttribute ConsultarRecomendacoesRequestDto filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.buscar(filtro, page, size);
    }
}