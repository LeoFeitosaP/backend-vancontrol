package com.VanControl.VanControl.grafo.service;

import com.VanControl.VanControl.grafo.domain.entity.ArestaPassageiroVeiculo;
import com.VanControl.VanControl.grafo.repository.GeografiaRepository;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmparelhamentoService {

    private final GeografiaRepository geografia;
    private final ViagemPassageiroRepository passageiros;

    public List<ArestaPassageiroVeiculo> recomendarViagensParaPassageiro(
            Passageiro passageiro,
            List<Viagem> viagens
    ) {
        var distancias = geografia.distanciasRodoviarias(
                passageiro.getId()
        );

        return viagens.stream()
                .filter(v -> v.getVeiculo() != null)
                .map(v -> {
                    long vagas = v.getVeiculo().getCapacidade()
                            - passageiros.countByViagem_Id(v.getId());

                    Double km = distancias.get(v.getId());

                    double peso = vagas <= 0 || km == null
                            ? Double.MAX_VALUE
                            : km / (1.0 + vagas);

                    return new ArestaPassageiroVeiculo(
                            passageiro.getId(),
                            v,
                            peso
                    );
                })
                .sorted(
                        Comparator.comparingDouble(
                                ArestaPassageiroVeiculo::peso
                        ).thenComparing(
                                a -> a.viagem().getCodigoViagem()
                        )
                )
                .toList();
    }
}