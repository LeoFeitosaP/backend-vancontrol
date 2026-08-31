package com.VanControl.VanControl.pagamento.controller;

import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.pagamento.domain.dto.request.AtualizarStatusPagamentoRequestDto;
import com.VanControl.VanControl.pagamento.domain.dto.request.CadastrarPagamentoRequestDto;
import com.VanControl.VanControl.pagamento.domain.dto.response.PagamentoDefaultResponseDto;
import com.VanControl.VanControl.pagamento.domain.dto.response.PagamentoResponseDto;
import com.VanControl.VanControl.pagamento.service.PagamentoService;
import com.VanControl.VanControl.user.domain.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Operacoes relacionadas a pagamentos")
@SecurityRequirement(name = "bearerAuth")
public class PagamentoController {

    private final PagamentoService pagamentoService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Cadastrar pagamento",
            description = "Entrada: CadastrarPagamentoRequestDto (cpf, competencia, valor, dataVencimento). Saida: PagamentoDefaultResponseDto com mensagem."
    )
    public ResponseEntity<PagamentoDefaultResponseDto> cadastroPagamento(@RequestBody @Valid CadastrarPagamentoRequestDto dto){
       return new ResponseEntity<>(pagamentoService.cadastrarPagamento(dto), HttpStatus.CREATED);
    }

    @PatchMapping("/{codigoPagamento}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Atualizar status do pagamento",
            description = "Entrada: codigoPagamento (path) e AtualizarStatusPagamentoRequestDto (status, dataPagamento). Saida: PagamentoDefaultResponseDto com mensagem."
    )
    public ResponseEntity<PagamentoDefaultResponseDto> atualizarStatusPagamento(@PathVariable String codigoPagamento, @RequestBody @Valid AtualizarStatusPagamentoRequestDto dto){
        return new ResponseEntity<>(pagamentoService.atualizarStatusPagamento(codigoPagamento, dto), HttpStatus.OK);
    }

    @GetMapping("/passageiro/{cpf}")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA','PASSAGEIRO')")
    @Operation(
            summary = "Listar pagamentos do passageiro",
            description = "Entrada: cpf do passageiro (path). Saida: lista de PagamentoResponseDto (nome, competencia, valor, dataVencimento, dataPagamento, status)."
    )
    public ResponseEntity<Page<PagamentoResponseDto>> buscarPagamentosDoPassageiroPorCpf(@PathVariable String cpf, @PageableDefault(size = 10, page = 0, sort = "dataVencimento", direction = Sort.Direction.DESC) Pageable pageable){
        securityUtils.validateCpfAccess(cpf);
        Page<PagamentoResponseDto> pagamentos = pagamentoService.buscarPagamentosDoPassageiroPorCpf(cpf, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(pagamentos);
    }

    @GetMapping("/{codigoPagamento}")
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Buscar pagamento por código",
            description = "Entrada: codigoPagamento (path). Saida: PagamentoResponseDto (nome, competencia, valor, dataVencimento, dataPagamento, status)."
    )
    public ResponseEntity<PagamentoResponseDto> buscarPagamentoPorCodigoPagamento(@PathVariable String codigoPagamento){
        return new ResponseEntity<>(pagamentoService.buscarPagamentoPorCodigoPagamento(codigoPagamento), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MOTORISTA')")
    @Operation(
            summary = "Buscar pagamentos por competencia",
            description = "Entrada: competencia (query, formato MM/yyyy). Saida: lista de PagamentoResponseDto (nome, competencia, valor, dataVencimento, dataPagamento, status)."
    )
    public ResponseEntity<Page<PagamentoResponseDto>> buscarPagamentosPorCompetencia(@RequestParam String competencia, @PageableDefault(size = 10, page = 0, sort = "dataVencimento", direction = Sort.Direction.DESC) Pageable pageable){
        return new ResponseEntity<>(pagamentoService.buscarPagamentosPorCompetencia(competencia, pageable),HttpStatus.OK);
    }

    @GetMapping("/meus-pagamentos")
    @PreAuthorize("hasRole('PASSAGEIRO')")
    @Operation(
            summary = "Listar meus pagamentos",
            description = "Saida: lista de PagamentoResponseDto (nome, competencia, valor, dataVencimento, dataPagamento, status) do usuario autenticado."
    )
    public ResponseEntity<Page<PagamentoResponseDto>> buscarMeusPagamentos(Pageable pageable){
        User userLogged = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        assert userLogged != null;
        Page<PagamentoResponseDto> pagamentos = pagamentoService.buscarMeusPagamentos(userLogged.getId(), pageable);

        return ResponseEntity.status(HttpStatus.OK).body(pagamentos);
    }

    @DeleteMapping("/{codigoPagamento}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Deletar pagamento",
            description = "Entrada: codigoPagamento (path). Saida: PagamentoDefaultResponseDto com mensagem."
    )
    public ResponseEntity<PagamentoDefaultResponseDto> deletarPagamento(@PathVariable String codigoPagamento){
        return new ResponseEntity<>(pagamentoService.deletarPagamento(codigoPagamento), HttpStatus.OK);
    }

}
