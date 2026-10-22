package com.VanControl.VanControl.veiculo.controller;

import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.VanControl.VanControl.veiculo.domain.dto.request.AtualizarStatusVeiculoRequestDto;
import com.VanControl.VanControl.veiculo.domain.dto.request.CadastrarVeiculoRequestDto;
import com.VanControl.VanControl.veiculo.domain.dto.response.VeiculoDefaultResponseDto;
import com.VanControl.VanControl.veiculo.domain.dto.response.VeiculoResponseDto;
import com.VanControl.VanControl.veiculo.service.VeiculoService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veiculos", description = "Veículos e endereço da base")
@SecurityRequirement(name = "bearerAuth")
public class VeiculoController {

    private final VeiculoService veiculoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Cadastrar veículo com endereço obrigatório da base"
    )
    public ResponseEntity<VeiculoDefaultResponseDto> cadastrarVeiculo(
            @RequestBody @Valid CadastrarVeiculoRequestDto dto
    ) {
        return new ResponseEntity<>(
                veiculoService.cadastrarVeiculo(dto),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{placa}")
    @Operation(summary = "Consultar veículo e endereço da base")
    public ResponseEntity<VeiculoResponseDto> buscarVeiculoPorPlaca(
            @PathVariable String placa
    ) {
        return ResponseEntity.ok(
                veiculoService.buscarVeiculoPorPlaca(placa)
        );
    }

    @GetMapping
    @Operation(summary = "Listar veículos e endereços")
    public ResponseEntity<Page<VeiculoResponseDto>> listarVeiculos(
            @PageableDefault(
                    size = 10,
                    page = 0,
                    sort = "placa",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                veiculoService.listarVeiculos(pageable)
        );
    }

    @PutMapping("/{placa}/endereco")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Substituir endereço completo da base",
            description = "Número obrigatório; omitir complemento o remove"
    )
    public ResponseEntity<VeiculoResponseDto> atualizarEndereco(
            @PathVariable String placa,
            @RequestBody @Valid EnderecoRequestDto dto
    ) {
        return ResponseEntity.ok(
                veiculoService.atualizarEndereco(placa, dto)
        );
    }

    @PutMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    public ResponseEntity<VeiculoDefaultResponseDto> atualizarStatusVeiculo(
            @RequestBody @Valid AtualizarStatusVeiculoRequestDto dto
    ) {
        return ResponseEntity.ok(
                veiculoService.atualizarStatusVeiculo(dto)
        );
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    public ResponseEntity<VeiculoDefaultResponseDto> deletarVeiculo(
            @RequestParam String placa
    ) {
        return ResponseEntity.ok(
                veiculoService.deletarVeiculo(placa)
        );
    }
}