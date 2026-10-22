package com.VanControl.VanControl.veiculo.mapper;

import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.endereco.domain.dto.EnderecoResponseDto;
import com.VanControl.VanControl.veiculo.domain.dto.request.CadastrarVeiculoRequestDto;
import com.VanControl.VanControl.veiculo.domain.dto.response.AdminVeiculoResponseDto;
import com.VanControl.VanControl.veiculo.domain.dto.response.VeiculoResponseDto;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;

import java.util.Locale;

public class VeiculoMapper {

    public static Veiculo converterParaVeiculo(
            CadastrarVeiculoRequestDto dto,
            Endereco endereco
    ) {
        var veiculo = new Veiculo(
                dto.placa(),
                dto.marca(),
                dto.modelo(),
                dto.ano(),
                dto.capacidade(),
                dto.renavam(),
                dto.status().toUpperCase(Locale.ROOT)
        );

        veiculo.definirEndereco(endereco);

        return veiculo;
    }

    public static VeiculoResponseDto converterParaVeiculoDto(Veiculo v) {
        return new VeiculoResponseDto(
                v.getPlaca(),
                v.getMarca(),
                v.getModelo(),
                v.getAno(),
                v.getCapacidade(),
                v.getStatus().name(),
                EnderecoResponseDto.de(v.getEnderecoBase()),
                v.getEnderecoInicial(),
                v.getEnderecoBase() == null
        );
    }

    public static AdminVeiculoResponseDto converterParaAdminVeiculoDto(
            Veiculo v
    ) {
        return new AdminVeiculoResponseDto(
                v.getIdVeiculo(),
                v.getPlaca(),
                v.getMarca(),
                v.getModelo(),
                v.getAno(),
                v.getCapacidade(),
                v.getRenavam(),
                v.getStatus().name(),
                EnderecoResponseDto.de(v.getEnderecoBase()),
                v.getEnderecoInicial(),
                v.getEnderecoBase() == null
        );
    }
}