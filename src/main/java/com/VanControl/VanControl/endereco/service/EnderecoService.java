package com.VanControl.VanControl.endereco.service;

import com.VanControl.VanControl.endereco.domain.dto.CepResponseDto;
import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.endereco.exceptions.EnderecoException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final ViaCepClient viaCep;

    public Endereco resolver(EnderecoRequestDto dto) {
        if (dto == null) {
            throw invalido("Informe o endereço");
        }

        String numero = texto(
                dto.numero(),
                "número",
                20,
                true
        );

        String complemento = texto(
                dto.complemento(),
                "complemento",
                100,
                false
        );

        CepResponseDto cep = viaCep.consultar(dto.cep());

        return new Endereco(
                cep.cep(),

                conciliar(
                        "logradouro",
                        dto.logradouro(),
                        cep.logradouro(),
                        200
                ),

                numero,
                complemento,

                conciliar(
                        "bairro",
                        dto.bairro(),
                        cep.bairro(),
                        100
                ),

                conciliar(
                        "cidade",
                        dto.cidade(),
                        cep.cidade(),
                        100
                ),

                conciliar(
                        "UF",
                        dto.uf(),
                        cep.uf(),
                        2
                ).toUpperCase(Locale.ROOT)
        );
    }

    private String conciliar(
            String campo,
            String informado,
            String oficial,
            int limite
    ) {
        String manual = texto(
                informado,
                campo,
                limite,
                false
        );

        if (oficial == null || oficial.isBlank()) {
            return texto(
                    manual,
                    campo,
                    limite,
                    true
            );
        }

        if (manual != null
                && !comparar(manual).equals(comparar(oficial))) {
            throw new EnderecoException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O campo " + campo
                            + " diverge do ViaCEP. Confira o CEP e o endereço informado"
            );
        }

        return oficial;
    }

    private String texto(
            String valor,
            String campo,
            int limite,
            boolean obrigatorio
    ) {
        String texto = valor == null ? null : valor.trim();

        if (texto == null || texto.isEmpty()) {
            if (obrigatorio) {
                throw invalido(
                        "Informe o campo " + campo + " do endereço"
                );
            }

            return null;
        }

        if (texto.length() > limite
                || texto.chars().anyMatch(Character::isISOControl)) {
            throw invalido(
                    "Valor inválido para o campo " + campo + " do endereço"
            );
        }

        return texto;
    }

    private String comparar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private EnderecoException invalido(String mensagem) {
        return new EnderecoException(
                HttpStatus.BAD_REQUEST,
                mensagem
        );
    }
}