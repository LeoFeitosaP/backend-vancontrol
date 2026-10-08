package com.VanControl.VanControl.veiculo.domain.dto.response;

import com.VanControl.VanControl.endereco.domain.dto.EnderecoResponseDto;

public record VeiculoResponseDto(
        String placa,
        String marca,
        String modelo,
        int ano,
        int capacidade,
        String status,
        EnderecoResponseDto endereco,
        String enderecoCompleto,
        boolean enderecoPendente
) {

    public VeiculoResponseDto(
            String placa,
            String marca,
            String modelo,
            int ano,
            int capacidade,
            String status
    ) {
        this(
                placa,
                marca,
                modelo,
                ano,
                capacidade,
                status,
                null,
                null,
                true
        );
    }
}