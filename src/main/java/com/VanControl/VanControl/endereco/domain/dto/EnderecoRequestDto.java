package com.VanControl.VanControl.endereco.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequestDto(

        @NotBlank(message = "Informe o CEP")
        @Pattern(
                regexp = "^[0-9]{5}-?[0-9]{3}$",
                message = "CEP deve conter 8 dígitos, com hífen opcional"
        )
        String cep,

        @Size(max = 200)
        String logradouro,

        @NotBlank(message = "Informe o número do endereço")
        @Size(max = 20)
        String numero,

        @Size(max = 100)
        String complemento,

        @Size(max = 100)
        String bairro,

        @Size(max = 100)
        String cidade,

        @Pattern(
                regexp = "(?i)^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$",
                message = "UF inválida"
        )
        String uf
) {
}
