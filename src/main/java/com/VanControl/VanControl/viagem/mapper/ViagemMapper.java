package com.VanControl.VanControl.viagem.mapper;

import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.rota.domain.entity.Rota;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.viagem.domain.dto.request.CriarViagemRequestDto;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResponseDto;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;

public class ViagemMapper {

    public static Viagem converterParaViagem(
            CriarViagemRequestDto dto,
            Rota rota,
            Veiculo veiculo,
            Motorista motorista
    ) {
        return new Viagem(
                rota,
                veiculo,
                motorista,
                dto.dataViagem(),
                dto.horarioSaidaPrevisto(),
                dto.horarioChegadaPrevisto()
        );
    }

    public static ViagemResponseDto converterParaViagemDto(Viagem viagem) {
        return new ViagemResponseDto(
                viagem.getCodigoViagem(),
                viagem.getCodigoRota(),
                viagem.getPlacaVeiculo(),
                viagem.getDocumentoMotorista(),
                viagem.getDataViagem(),
                viagem.getHorarioSaidaPrevisto(),
                viagem.getHorarioChegadaPrevisto(),
                viagem.isViagemConcluida()
        );
    }
}