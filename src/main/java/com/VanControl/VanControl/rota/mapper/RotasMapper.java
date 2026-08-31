package com.VanControl.VanControl.rota.mapper;

import com.VanControl.VanControl.rota.domain.dto.request.CadastrarRotaRequestDto;
import com.VanControl.VanControl.rota.domain.dto.response.RotaResponseDto;
import com.VanControl.VanControl.rota.domain.entity.Rota;

public class RotasMapper {

    public static Rota converterParaRota(CadastrarRotaRequestDto dto){
        return new Rota(
                dto.descricao(),
                dto.destino(),
                dto.distancia(),
                dto.tempoEstimado()
        );
    }

    public static RotaResponseDto converterParaRotaDto(Rota rota){
        return new RotaResponseDto(
                rota.getCodigoRota(),
                rota.getDescricao(),
                rota.getDestino(),
                rota.getDistancia(),
                rota.getTempoEstimado()
        );
    }
}
