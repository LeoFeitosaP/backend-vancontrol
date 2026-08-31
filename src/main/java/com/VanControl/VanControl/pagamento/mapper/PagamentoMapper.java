package com.VanControl.VanControl.pagamento.mapper;

import com.VanControl.VanControl.pagamento.domain.dto.request.CadastrarPagamentoRequestDto;
import com.VanControl.VanControl.pagamento.domain.dto.response.PagamentoResponseDto;
import com.VanControl.VanControl.pagamento.domain.entity.Pagamento;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;


public class PagamentoMapper {

    public static Pagamento converterParaPagamento(CadastrarPagamentoRequestDto dto, Passageiro passageiro){
       Pagamento pagamento = new Pagamento();

       pagamento.setPassageiro(passageiro);
       pagamento.setCompetencia(dto.competencia());
       pagamento.setValor(dto.valor());
       pagamento.setDataVencimento(dto.dataVencimento());

        return pagamento;
    }

    public static PagamentoResponseDto converterParaPagamentoDto(Pagamento pagamento){
        return new PagamentoResponseDto(
                pagamento.getCodigoPagamento(),
                pagamento.getPassageiro().getUser().getName(),
                pagamento.getCompetencia(),
                pagamento.getValor(),
                pagamento.getDataVencimento(),
                pagamento.getDataPagamento(),
                pagamento.getStatus()
        );
    }
}
