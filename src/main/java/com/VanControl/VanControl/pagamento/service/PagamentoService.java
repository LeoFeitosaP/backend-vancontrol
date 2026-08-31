package com.VanControl.VanControl.pagamento.service;

import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.pagamento.domain.dto.request.AtualizarStatusPagamentoRequestDto;
import com.VanControl.VanControl.pagamento.domain.dto.request.CadastrarPagamentoRequestDto;
import com.VanControl.VanControl.pagamento.domain.dto.response.PagamentoDefaultResponseDto;
import com.VanControl.VanControl.pagamento.domain.dto.response.PagamentoResponseDto;
import com.VanControl.VanControl.pagamento.domain.entity.Pagamento;
import com.VanControl.VanControl.pagamento.domain.enums.StatusPagamento;
import com.VanControl.VanControl.pagamento.mapper.PagamentoMapper;
import com.VanControl.VanControl.pagamento.repository.PagamentoRepository;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagamentoService{

    private final PagamentoRepository pagamentoRepository;
    private final PassageiroRepository passageiroRepository;

    public PagamentoDefaultResponseDto cadastrarPagamento(CadastrarPagamentoRequestDto dto) {

            var passageiro = passageiroRepository.findByUser_Cpf(dto.cpf());

        if(passageiro.isEmpty()){
            throw new NotFoundException("Passageiro não encontrado");
        }

        if (pagamentoRepository.existsByPassageiroIdAndCompetencia(passageiro.get().getId(), dto.competencia())) {
            throw new ConflictException("Pagamento já cadastrado para o passageiro na competência: " + dto.competencia());
        }

        var pagamento = PagamentoMapper.converterParaPagamento(dto, passageiro.get());
        pagamento.setStatus(StatusPagamento.PENDENTE);

        pagamento.setCodigoPagamento("PGTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        pagamentoRepository.save(pagamento);

        return new PagamentoDefaultResponseDto("Pagamento cadastrado com sucesso");
    }

    public PagamentoDefaultResponseDto atualizarStatusPagamento(String codigoPagamento, AtualizarStatusPagamentoRequestDto dto){

        Pagamento pagamento = pagamentoRepository.findByCodigoPagamento(codigoPagamento)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado"));

        pagamento.setStatus(StatusPagamento.valueOf(dto.status()));
        pagamento.setDataPagamento(dto.dataPagamento());

        pagamentoRepository.save(pagamento);

        return new PagamentoDefaultResponseDto("Status e data de pagamento atualizado com sucesso");
    }

    public Page<PagamentoResponseDto> buscarPagamentosDoPassageiroPorCpf(String cpf, Pageable pageable){
        var passageiro = passageiroRepository.findByUser_Cpf(cpf)
                .orElseThrow(() -> new NotFoundException("Passageiro não encontrado"));

        var pagamentos = pagamentoRepository.findByPassageiroId(passageiro.getId(), pageable);
        if (pagamentos.isEmpty()) {
            throw new NotFoundException("Nenhum pagamento encontrado para esse passageiro");
        }

        return pagamentos.map(PagamentoMapper::converterParaPagamentoDto);
    }

    public PagamentoResponseDto buscarPagamentoPorCodigoPagamento(String codigoPagamento){
        Pagamento pagamento = pagamentoRepository.findByCodigoPagamento(codigoPagamento)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado"));

        return  PagamentoMapper.converterParaPagamentoDto(pagamento);
    }

    public Page<PagamentoResponseDto> buscarPagamentosPorCompetencia(String competencia, Pageable pageable){
        var pagamento = pagamentoRepository.findByCompetencia(competencia, pageable);
        if(pagamento.isEmpty()){
            throw new NotFoundException("Competencia não encontrado");
        }
        return pagamento.map(PagamentoMapper::converterParaPagamentoDto);
    }

    public Page<PagamentoResponseDto> buscarMeusPagamentos(UUID user, Pageable pageable){

        Passageiro passageiro = passageiroRepository.findByUser_Id(user)
                .orElseThrow(() -> new NotFoundException("Perfil de passageiro não encontrado para esse usuário"));

        Page<Pagamento> pagamentos = pagamentoRepository.findByPassageiroId(passageiro.getId(), pageable);

        return pagamentos.map(PagamentoMapper::converterParaPagamentoDto);
    }

    @Transactional
    public PagamentoDefaultResponseDto deletarPagamento(String codigoPagamento){
        var pagamento = pagamentoRepository.findByCodigoPagamento(codigoPagamento);
        if(pagamento.isEmpty()){
            throw new NotFoundException("Pagamento não encontrado");
        }

        pagamentoRepository.delete(pagamento.get());
        return new PagamentoDefaultResponseDto("Pagamento removido com sucesso!");
    }

}
