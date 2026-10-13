package com.VanControl.VanControl.grafo.controller;

import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Distancia;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.DistanciaRequest;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Geocodificacao;
import com.VanControl.VanControl.grafo.service.NominatimClient;
import com.VanControl.VanControl.grafo.service.OsrmClient;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/geolocalizacao")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Geolocalização",
        description = "Consultas administrativas sem alteração de cadastros"
)
public class GeolocalizacaoController {

    private final EnderecoService enderecos;
    private final NominatimClient nominatim;
    private final OsrmClient osrm;

    @PostMapping("/enderecos/consultar")
    public ResponseEntity<Geocodificacao> consultarEndereco(
            @RequestBody @Valid EnderecoRequestDto dto
    ) {
        var resultado = nominatim.consultar(enderecos.resolver(dto));

        int status = switch (resultado.estado()) {
            case CONFIRMADA -> 200;
            case ENDERECO_INCOMPLETO -> 400;
            case NAO_ENCONTRADA, AMBIGUA, IMPRECISA -> 422;
            case RESPOSTA_INVALIDA -> 502;
            case INDISPONIVEL, NAO_CONFIGURADO -> 503;
        };

        return ResponseEntity.status(status).body(resultado);
    }

    @PostMapping("/distancias/consultar")
    public ResponseEntity<Distancia> consultarDistancia(
            @RequestBody @Valid DistanciaRequest dto
    ) {
        var resultado = osrm.consultar(
                dto.origem(),
                dto.destino(),
                dto.permitirEstimativa()
        );

        int status = switch (resultado.estado()) {
            case CALCULADA, ESTIMADA -> 200;
            case SEM_ROTA, PONTO_FORA_DA_VIA -> 422;
            case RESPOSTA_INVALIDA -> 502;
            case INDISPONIVEL, NAO_CONFIGURADO -> 503;
        };

        return ResponseEntity.status(status).body(resultado);
    }
}