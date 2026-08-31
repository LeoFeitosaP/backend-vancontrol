package com.VanControl.VanControl.motorista.mapper;

import com.VanControl.VanControl.motorista.domain.dto.request.CadastrarMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaResponseDto;
import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;

public class MotoristaMapper {

    public static Motorista converterParaMotorista(CadastrarMotoristaRequestDto dto) {
        return new Motorista(
                dto.cnh(),
                dto.categoriaCnh(),
                dto.dataValidadeCnh(),
                dto.telefone()
        );
    }

    public static MotoristaResponseDto converterParaMotoristaDto(Motorista motorista) {
        return new MotoristaResponseDto(
                motorista.getUser() != null ? motorista.getUser().getName() : null,
                motorista.getUser() != null ? motorista.getUser().getCpf() : null,
                motorista.getUser() != null ? motorista.getUser().getEmail() : null,
                motorista.getCnh(),
                motorista.getCategoriaCnh(),
                motorista.getDataValidadeCnh(),
                motorista.getTelefone()
        );
    }

    public static RegisterRequestDTO converterParaRequestDto(CadastrarMotoristaRequestDto dto) {
        return new RegisterRequestDTO(
                dto.nome(),
                dto.email(),
                dto.password(),
                dto.cpf(),
                dto.telefone(),
                null,
                null,
                null,
                null
        );
    }
}
