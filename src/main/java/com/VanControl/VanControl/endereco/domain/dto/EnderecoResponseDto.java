package com.VanControl.VanControl.endereco.domain.dto;

import com.VanControl.VanControl.endereco.domain.entity.Endereco;

public record EnderecoResponseDto(
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf
) {

    public static EnderecoResponseDto de(Endereco endereco) {
        if (endereco == null) {
            return null;
        }

        return new EnderecoResponseDto(
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf()
        );
    }
}