package com.VanControl.VanControl.passageiro.mapper;

import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroResponseDto;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;

public class PassageiroMapper {

    public static Passageiro converterParaPassageiro(RegisterRequestDTO dto) {
        return new Passageiro(
                dto.telefone(),
                dto.instituicaoEnsino(),
                dto.turno(),
                dto.endereco(),
                dto.cep()
        );
    }

    public static PassageiroResponseDto converterParaPassageiroResponseDto(Passageiro passageiro) {
        return new PassageiroResponseDto(
                passageiro.getUser() != null ? passageiro.getUser().getName() : null,
                passageiro.getUser() != null ? passageiro.getUser().getCpf() : null,
                passageiro.getTelefone(),
                passageiro.getUser() != null ? passageiro.getUser().getEmail() : null,
                passageiro.getInstituicaoEnsino(),
                passageiro.getTurno(),
                passageiro.getEndereco(),
                passageiro.getCep()
        );
    }
}
