package com.VanControl.VanControl.rota.controller;

import com.VanControl.VanControl.rota.domain.dto.request.AtualizarDescricaoRotaRequestDto;
import com.VanControl.VanControl.rota.domain.dto.request.CadastrarRotaRequestDto;
import com.VanControl.VanControl.rota.domain.dto.response.RotaDefaultResponseDto;
import com.VanControl.VanControl.rota.domain.dto.response.RotaResponseDto;
import com.VanControl.VanControl.rota.service.RotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rotas")
@RequiredArgsConstructor
@Tag(name = "Rotas", description = "Operacoes relacionadas a rotas")
@SecurityRequirement(name = "bearerAuth")
public class RotaController {

    private final RotaService rotaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Cadastrar rota",
            description = "Entrada: CadastrarRotaRequestDto (descricao, destino, distancia, tempoEstimado). Saida: RotaDefaultResponseDto com mensagem."
    )
    public ResponseEntity<RotaDefaultResponseDto> cadastrarRota(@RequestBody @Valid CadastrarRotaRequestDto dto) {
       return new ResponseEntity<>(rotaService.cadastrarRota(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{destino}")
    @Operation(
            summary = "Buscar rotas por destino",
            description = "Entrada: destino (path). Saida: lista de RotaResponseDto (codigoRota, descricao, destino, distancia, tempoEstimado)."
    )
    public ResponseEntity<Page<RotaResponseDto>> buscarRotaPorDestino(@PathVariable String destino, @PageableDefault(size = 10, page = 0, sort = "codigoRota", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<RotaResponseDto> rotas = rotaService.buscarRotaPorDestino(destino, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(rotas);
    }

    @GetMapping
    @Operation(
            summary = "Listar todas as rotas",
            description = "Saida: lista de RotaResponseDto (codigoRota, descricao, destino, distancia, tempoEstimado)."
    )
    public ResponseEntity<Page<RotaResponseDto>> buscarTodasRotas(@PageableDefault(size = 10, page = 0, sort = "codigoRota", direction = Sort.Direction.ASC) Pageable pageable){
        return new ResponseEntity<>(rotaService.buscarTodasRotas(pageable), HttpStatus.OK);
    }

    @PatchMapping("/{codigoRota}/descricao")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Atualizar descricao da rota",
            description = "Entrada: codigoRota (path) e AtualizarDescricaoRotaRequestDto (novaDescricao). Saida: RotaDefaultResponseDto com mensagem."
    )
    public ResponseEntity<RotaDefaultResponseDto> atualizarDescricaoRota(@PathVariable String codigoRota, @RequestBody AtualizarDescricaoRotaRequestDto dto) {
        RotaDefaultResponseDto response = rotaService.atualizarDescricaoRota(codigoRota, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping()
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Deletar rota",
            description = "Entrada: codigoRota (query). Saida: RotaDefaultResponseDto com mensagem."
    )
    public ResponseEntity<RotaDefaultResponseDto> deletarRota(@RequestParam String codigoRota) {
        return new ResponseEntity<>(rotaService.deletarRota(codigoRota), HttpStatus.OK);
    }

}
