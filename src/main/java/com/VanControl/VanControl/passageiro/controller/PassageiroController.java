package com.VanControl.VanControl.passageiro.controller;

import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.passageiro.domain.dto.request.AtualizarPassageiroRequestDto;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroDefaultResponseDto;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroResponseDto;
import com.VanControl.VanControl.passageiro.service.PassageiroService;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResumoResponseDto;
import com.VanControl.VanControl.viagem.service.ViagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/passageiros")
@RequiredArgsConstructor
@Tag(name = "Passageiros", description = "Operacoes relacionadas a passageiros")
@SecurityRequirement(name = "bearerAuth")
public class PassageiroController {

    private final PassageiroService passageiroService;
    private final SecurityUtils securityUtils;
    private final ViagemService viagemService;

    @GetMapping("/{cpf}")
    @Operation(
            summary = "Buscar passageiro por CPF",
            description = "Entrada: cpf (path). Saida: PassageiroResponseDto (nome, cpf, telefone, email, intituicaoEnsino, turno, endereco, cep)."
    )
    public ResponseEntity<PassageiroResponseDto> buscarPassageiroPorCpf(@PathVariable String cpf) {
        securityUtils.validateCpfAccess(cpf);
        return new ResponseEntity<>(passageiroService.buscarPassageiroPorCpf(cpf), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Listar passageiros",
            description = "Saida: lista de PassageiroResponseDto (nome, cpf, telefone, email, intituicaoEnsino, turno, endereco, cep)."
    )
    public ResponseEntity<Page<PassageiroResponseDto>> buscarTodosPassageiros(@PageableDefault(size = 10, page = 0, sort = "user.name", direction = org.springframework.data.domain.Sort.Direction.ASC) Pageable pageable) {
        return new ResponseEntity<>(passageiroService.listarPassageiros(pageable), HttpStatus.OK);
    }

    @PutMapping("/{cpf}")
    @PreAuthorize("hasAnyRole('ADMIN','PASSAGEIRO')")
    @Operation(
            summary = "Atualizar passageiro",
            description = "Entrada: cpf (path) e AtualizarPassageiroRequestDto (nome, telefone, email, intituicaoEnsino, turno, Endereco, cep). Saida: PassageiroResponseDto."
    )
    public ResponseEntity<PassageiroResponseDto> atualizarPassageiro(@PathVariable String cpf, @RequestBody @Valid AtualizarPassageiroRequestDto dto) {
        securityUtils.validateCpfAccess(cpf);
        return new ResponseEntity<>(passageiroService.atualizarPassageiro(cpf, dto), HttpStatus.OK);
    }

    @DeleteMapping("/{cpf}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Deletar passageiro",
            description = "Entrada: cpf (path). Saida: PassageiroDefaultResponseDto com mensagem."
    )
    public ResponseEntity<PassageiroDefaultResponseDto> deletarPassageiro(@PathVariable String cpf) {
        return new ResponseEntity<>(passageiroService.deletarPassageiro(cpf), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{cpf}/viagens")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA','PASSAGEIRO')")
    @Operation(
            summary = "Listar viagens do passageiro",
            description = "Entrada: cpf (path). Saida: lista de ViagemResumoResponseDto."
    )
    public ResponseEntity<Page<ViagemResumoResponseDto>> listarViagensPorPassageiro(@PathVariable String cpf, @PageableDefault(size = 10, page = 0, sort = "dataViagem", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        securityUtils.validateCpfAccess(cpf);
        return new ResponseEntity<>(viagemService.listarViagensPorPassageiroCpf(cpf, pageable), HttpStatus.OK);
    }
}
