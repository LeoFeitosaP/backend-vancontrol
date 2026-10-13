package com.VanControl.VanControl.grafo.service;

import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.grafo.domain.dto.GeografiaPersistida.*;
import com.VanControl.VanControl.grafo.repository.GeografiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoLocalizacaoService {

    private final GeografiaRepository repository;
    private final NominatimClient nominatim;
    private final OsrmClient osrm;
    private final EnderecoService enderecos;

    public Localizacao consultar(Tipo tipo, String chave) {
        return repository.buscar(tipo, chave);
    }

    public Localizacao processar(
            Tipo tipo,
            String chave,
            boolean forcar
    ) {
        var atual = consultar(tipo, chave);

        if (!forcar && atual.consultadoEm() != null) {
            return atual;
        }

        var resultado = nominatim.consultar(atual.endereco());

        // Nenhuma transação/lock de banco fica aberta durante a chamada externa.
        return repository.salvarResultado(atual, resultado);
    }

    public Localizacao revisarOrigem(
            String codigo,
            RevisarOrigem dto
    ) {
        consultar(Tipo.VIAGEM, codigo);

        var endereco = enderecos.resolver(dto.endereco());

        return repository.revisarOrigem(
                codigo,
                endereco,
                dto.justificativa()
        );
    }

    public Calculo distancia(
            String codigo,
            String cpf,
            boolean estimativa,
            boolean processar,
            boolean forcar
    ) {
        var origem = consultar(Tipo.VIAGEM, codigo);
        var destino = consultar(Tipo.PASSAGEIRO, cpf);

        if (!"CONFIRMADA".equals(origem.estado())
                || !"CONFIRMADA".equals(destino.estado())) {
            throw new ConflictException(
                    "Geocodifique a origem da viagem e o passageiro antes de calcular"
            );
        }

        var cache = repository.consultarDistancia(
                origem,
                destino,
                estimativa
        );

        if (cache.isPresent() && (!processar || !forcar)) {
            return cache.get();
        }

        if (!processar) {
            throw new ConflictException(
                    "Distância ausente ou expirada; solicite o cálculo"
            );
        }

        var resultado = osrm.consultar(
                origem.coordenadas(),
                destino.coordenadas(),
                estimativa
        );

        return repository.salvarDistancia(
                origem,
                destino,
                estimativa,
                resultado
        );
    }
}