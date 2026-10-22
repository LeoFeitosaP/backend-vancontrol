package com.VanControl.VanControl.grafo.controller;

import com.VanControl.VanControl.grafo.domain.dto.GeografiaPersistida.*;
import com.VanControl.VanControl.grafo.service.GeoLocalizacaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/geolocalizacao/cadastros")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class GeografiaPersistidaController {

    private final GeoLocalizacaoService service;

    @GetMapping("/{tipo}/{chave}")
    public Localizacao consultar(
            @PathVariable Tipo tipo,
            @PathVariable String chave
    ) {
        return service.consultar(tipo, chave);
    }

    @PostMapping("/{tipo}/{chave}/processar")
    public ResponseEntity<Localizacao> processar(
            @PathVariable Tipo tipo,
            @PathVariable String chave,
            @RequestParam(defaultValue = "false") boolean forcar
    ) {
        var r = service.processar(tipo, chave, forcar);

        int status = switch (r.estado()) {
            case "CONFIRMADA", "IMPRECISA" -> 200;
            case "INDISPONIVEL", "NAO_CONFIGURADO" -> 503;
            case "RESPOSTA_INVALIDA" -> 502;
            default -> 422;
        };

        return ResponseEntity.status(status).body(r);
    }

    @PutMapping("/viagens/{codigo}/origem")
    public Localizacao revisarOrigem(
            @PathVariable String codigo,
            @RequestBody @Valid RevisarOrigem dto
    ) {
        return service.revisarOrigem(codigo, dto);
    }

    @GetMapping("/viagens/{codigo}/passageiros/{cpf}/distancia")
    public ResponseEntity<Calculo> consultarDistancia(
            @PathVariable String codigo,
            @PathVariable String cpf,
            @RequestParam(defaultValue = "false") boolean permitirEstimativa
    ) {
        return resposta(
                service.distancia(
                        codigo,
                        cpf,
                        permitirEstimativa,
                        false,
                        false
                )
        );
    }

    @PostMapping("/viagens/{codigo}/passageiros/{cpf}/distancia")
    public ResponseEntity<Calculo> calcularDistancia(
            @PathVariable String codigo,
            @PathVariable String cpf,
            @RequestParam(defaultValue = "false") boolean permitirEstimativa,
            @RequestParam(defaultValue = "false") boolean forcar
    ) {
        return resposta(
                service.distancia(
                        codigo,
                        cpf,
                        permitirEstimativa,
                        true,
                        forcar
                )
        );
    }

    private ResponseEntity<Calculo> resposta(Calculo calculo) {
        int status = switch (calculo.resultado().estado()) {
            case CALCULADA, ESTIMADA -> 200;
            case SEM_ROTA, PONTO_FORA_DA_VIA -> 422;
            case RESPOSTA_INVALIDA -> 502;
            case INDISPONIVEL, NAO_CONFIGURADO -> 503;
        };

        return ResponseEntity.status(status).body(calculo);
    }
}