package com.VanControl.VanControl.veiculo.mapper;

import com.VanControl.VanControl.veiculo.domain.dto.request.CadastrarVeiculoRequestDto;
import com.VanControl.VanControl.veiculo.domain.dto.response.AdminVeiculoResponseDto;
import com.VanControl.VanControl.veiculo.domain.dto.response.VeiculoResponseDto;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;

public class VeiculoMapper {

    public static Veiculo converterParaVeiculo(CadastrarVeiculoRequestDto dto) {
        return new Veiculo(
                dto.placa(),
                dto.marca(),
                dto.modelo(),
                dto.ano(),
                dto.capacidade(),
                dto.renavam(),
                dto.status().toUpperCase()
        );
    }

    public static VeiculoResponseDto converterParaVeiculoDto(Veiculo veiculo) {
        return new VeiculoResponseDto(
                veiculo.getPlaca(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getCapacidade(),
                String.valueOf(veiculo.getStatus())
        );
    }

    public static AdminVeiculoResponseDto converterParaAdminVeiculoDto(Veiculo veiculo) {
        return new AdminVeiculoResponseDto(
                veiculo.getIdVeiculo(),
                veiculo.getPlaca(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getCapacidade(),
                veiculo.getRenavam(),
                String.valueOf(veiculo.getStatus())
        );
    }
}
