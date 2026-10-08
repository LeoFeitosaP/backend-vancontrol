package com.VanControl.VanControl.endereco.domain.dto;

public record CepResponseDto(
        String cep,
        String logradouro,
        String bairro,
        String cidade,
        String uf,
        String complementoViaCep
) {
}