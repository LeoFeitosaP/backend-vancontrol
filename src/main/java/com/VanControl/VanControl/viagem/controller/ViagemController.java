package com.VanControl.VanControl.viagem.controller;

import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.viagem.domain.dto.request.CriarViagemRequestDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemDefaultResponseDto;
import com.VanControl.VanControl.viagemPassageiro.domain.dto.ViagemPassageirosResponseDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResponseDto;
import com.VanControl.VanControl.viagem.service.ViagemService;
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
@RequestMapping("/viagens")
@RequiredArgsConstructor
@Tag(name = "Viagens", description = "Operacoes relacionadas a viagens")
@SecurityRequirement(name = "bearerAuth")
public class ViagemController {

    private final ViagemService viagemService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Cadastrar viagem",
            description = "Entrada: CriarViagemRequestDto (codigoRota, placaVeiculo, cpfMotorista, dataViagem, horarioSaidaPrevisto, horarioChegadaPrevisto). Saida: ViagemDefaultResponseDto com mensagem."
    )
    public ResponseEntity<ViagemDefaultResponseDto> cadastrarViagem(@RequestBody @Valid CriarViagemRequestDto dto) {
        return new ResponseEntity<>(viagemService.cadastrarViagem(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA','PASSAGEIRO')")
    @Operation(
            summary = "Buscar viagem por codigo",
            description = "Entrada: codigo (path). Saida: ViagemResponseDto (codigoRota, placaVeiculo, cpfMotorista, dataViagem, horarioSaidaPrevisto, horarioChegadaPrevisto, viagemConcuida)."
    )
    public ResponseEntity<ViagemResponseDto> buscarViagemPorCodigo(@PathVariable String codigo) {
        return new ResponseEntity<>(viagemService.buscarViagemPorCodigo(codigo), HttpStatus.OK);
    }

    @PostMapping("/{codigo}/passageiros/{cpf}")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA','PASSAGEIRO')")
    @Operation(
            summary = "Associar passageiro a viagem",
            description = "Entrada: codigo e cpf (path). Saida: ViagemDefaultResponseDto com mensagem."
    )
    public ResponseEntity<ViagemDefaultResponseDto> associarPassageiro(@PathVariable String codigo, @PathVariable String cpf) {
        securityUtils.validateCpfAccess(cpf);
        return new ResponseEntity<>(viagemService.adicionarPassageiro(codigo, cpf), HttpStatus.OK);
    }

    @DeleteMapping("/{codigo}/passageiros/{cpf}")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA','PASSGEIRO')")
    @Operation(
            summary = "Remover passageiro da viagem",
            description = "Entrada: codigo e cpf (path). Saida: ViagemDefaultResponseDto com mensagem."
    )
    public ResponseEntity<ViagemDefaultResponseDto> removerPassageiro(@PathVariable String codigo, @PathVariable String cpf) {
        securityUtils.validateCpfAccess(cpf);
        return new ResponseEntity<>(viagemService.removerPassageiro(codigo, cpf), HttpStatus.OK);
    }

    @GetMapping("/{codigo}/passageiros")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Listar passageiros da viagem",
            description = "Entrada: codigo (path). Saida: ViagemPassageirosResponseDto com capacidade, ocupacao e lista de passageiros."
    )
    public ResponseEntity<ViagemPassageirosResponseDto> listarPassageiros(@PathVariable String codigo) {
        return new ResponseEntity<>(viagemService.listarPassageirosPorViagem(codigo), HttpStatus.OK);
    }

    @GetMapping
    @Operation(
            summary = "Listar todas as viagens",
            description = "Saida: lista de ViagemResponseDto (codigoRota, placaVeiculo, cpfMotorista, dataViagem, horarioSaidaPrevisto, horarioChegadaPrevisto, viagemConcuida)."
    )
    public ResponseEntity<Page<ViagemResponseDto>> listarTodasViagens(@PageableDefault(size = 10, page = 0, sort = "dataViagem", direction = Sort.Direction.DESC) Pageable pageable) {
        return new ResponseEntity<>(viagemService.listarTodasViagens(pageable), HttpStatus.OK);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Atualizar status da viagem",
            description = "Entrada: codigo (path). Saida: ViagemDefaultResponseDto com mensagem."
    )
    public ResponseEntity<ViagemDefaultResponseDto> atualizarStatusViagem(@PathVariable String codigo) {
        return new ResponseEntity<>(viagemService.atualizarStatusViagem(codigo), HttpStatus.OK);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Deletar viagem",
            description = "Entrada: codigo (path). Saida: ViagemDefaultResponseDto com mensagem."
    )
    public ResponseEntity<ViagemDefaultResponseDto> deletarViagem(@PathVariable String codigo) {
        return new ResponseEntity<>(viagemService.deletarViagem(codigo), HttpStatus.OK);
    }
}
