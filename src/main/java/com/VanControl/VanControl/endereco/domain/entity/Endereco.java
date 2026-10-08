package com.VanControl.VanControl.endereco.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Endereco {

    @Column(name = "endereco_cep", length = 8)
    private String cep;

    @Column(name = "endereco_logradouro", length = 200)
    private String logradouro;

    @Column(name = "endereco_numero", length = 20)
    private String numero;

    @Column(name = "endereco_complemento", length = 100)
    private String complemento;

    @Column(name = "endereco_bairro", length = 100)
    private String bairro;

    @Column(name = "endereco_cidade", length = 100)
    private String cidade;

    @Column(name = "endereco_uf", length = 2)
    private String uf;

    public String formatar() {
        return Stream.of(
                        logradouro,
                        numero,
                        complemento,
                        bairro,
                        cidade,
                        uf,
                        cep,
                        "Brasil"
                )
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }
}
