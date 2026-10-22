package com.VanControl.VanControl.motorista.controller;

import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.motorista.domain.dto.request.AtualizarTelefoneMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.request.CadastrarMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaDefaultResponseDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaResponseDto;
import com.VanControl.VanControl.motorista.service.MotoristaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/motoristas")
@RequiredArgsConstructor
@Tag(name = "Motoristas", description = "Operacoes relacionadas a motoristas")
@SecurityRequirement(name = "bearerAuth")
public class MotoristaController {

    private final MotoristaService motoristaService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotoristaDefaultResponseDto> cadastrarMotorista(@RequestBody @Valid CadastrarMotoristaRequestDto dto) {
        log.info("1 - Requisição de cadastro de motorista recebida");
        return new ResponseEntity<>(motoristaService.cadastrarMotorista(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<MotoristaResponseDto> buscarMotoristaPorCpf(@PathVariable String cpf) {
        log.info("1 - Requisição de busca de motorista com cpf {} recebida", cpf);
        securityUtils.validateCpfAccess(cpf);
        return new ResponseEntity<>(motoristaService.buscarMotoristaPorCpf(cpf), HttpStatus.OK);
    }
    
    @GetMapping
    public ResponseEntity<Page<MotoristaResponseDto>> buscarTodosMotoristas(@PageableDefault(size = 10, page = 0, sort = "user.name", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("1 - Requição de busca de todos os motoristas recebida");
        return new ResponseEntity<>(motoristaService.buscarTodosMotoristas(pageable), HttpStatus.OK);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    public ResponseEntity<MotoristaDefaultResponseDto> atualizarTelefoneMotorista(@RequestBody @Valid AtualizarTelefoneMotoristaRequestDto dto) {
        log.info("1 - Requisição de atualização de telefone do motorista com cpf {} recebida", dto.cpf());
        securityUtils.validateCpfAccess(dto.cpf());
        return new ResponseEntity<>(motoristaService.atualizarTelefoneMotorista(dto), HttpStatus.OK);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotoristaDefaultResponseDto> deletarMotorista(@RequestParam String cpf) {
        log.info("1 - Requisição de deleção do motorista com cpf {} recebida", cpf);
        return new ResponseEntity<>(motoristaService.deletarMotorista(cpf), HttpStatus.OK);
    }
}
