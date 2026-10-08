package com.VanControl.VanControl.endereco;

import com.VanControl.VanControl.endereco.domain.dto.CepResponseDto;
import com.VanControl.VanControl.endereco.service.ViaCepClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enderecos")
@RequiredArgsConstructor
@Tag(name = "Endereços")
@SecurityRequirements
public class EnderecoController {

    private final ViaCepClient viaCep;

    @GetMapping("/cep/{cep}")
    @Operation(
            summary = "Consultar CEP",
            description = "Consulta pública para o cadastro; número é informado pelo usuário"
    )
    public CepResponseDto consultar(@PathVariable String cep) {
        return viaCep.consultar(cep);
    }
}