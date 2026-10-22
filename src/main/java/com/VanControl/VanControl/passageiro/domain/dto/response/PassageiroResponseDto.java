package com.VanControl.VanControl.passageiro.domain.dto.response;

import com.VanControl.VanControl.endereco.domain.dto.EnderecoResponseDto;

public record PassageiroResponseDto(
        String nome,
        String cpf,
        String telefone,
        String email,
        String intituicaoEnsino,
        String turno,
        String endereco,
        String cep,
        EnderecoResponseDto enderecoDetalhado,
        boolean enderecoPendente
) {
    public PassageiroResponseDto(
            String nome,
            String cpf,
            String telefone,
            String email,
            String intituicaoEnsino,
            String turno,
            String endereco,
            String cep
    ) {
        this(
                nome,
                cpf,
                telefone,
                email,
                intituicaoEnsino,
                turno,
                endereco,
                cep,
                null,
                true
        );
    }
}