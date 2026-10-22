package com.VanControl.VanControl.passageiro.mapper;

import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.endereco.domain.dto.EnderecoResponseDto;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroResponseDto;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;

public class PassageiroMapper {

    public static Passageiro converterParaPassageiro(
            RegisterRequestDTO dto,
            Endereco endereco
    ) {
        var passageiro = new Passageiro();
        passageiro.setTelefone(dto.telefone());
        passageiro.setInstituicaoEnsino(dto.instituicaoEnsino());
        passageiro.setTurno(dto.turno());
        passageiro.definirEndereco(endereco);
        return passageiro;
    }

    public static PassageiroResponseDto converterParaPassageiroResponseDto(
            Passageiro passageiro
    ) {
        var user = passageiro.getUser();

        return new PassageiroResponseDto(
                user != null ? user.getName() : null,
                user != null ? user.getCpf() : null,
                passageiro.getTelefone(),
                user != null ? user.getEmail() : null,
                passageiro.getInstituicaoEnsino(),
                passageiro.getTurno(),
                passageiro.getEndereco(),
                passageiro.getCep(),
                EnderecoResponseDto.de(passageiro.getEnderecoDetalhado()),
                passageiro.getEnderecoDetalhado() == null
        );
    }
}