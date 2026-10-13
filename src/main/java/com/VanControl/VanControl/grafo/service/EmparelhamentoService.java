package com.VanControl.VanControl.grafo.service;

import com.VanControl.VanControl.grafo.domain.entity.ArestaPassageiroVeiculo;
import com.VanControl.VanControl.grafo.repository.GeografiaRepository;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagem.service.RegrasReserva;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmparelhamentoService {

    private final GeografiaRepository geografia;
    private final ViagemPassageiroRepository passageiros;

    @Transactional(readOnly = true)
    public List<ArestaPassageiroVeiculo> recomendarViagensParaPassageiro(
            Passageiro passageiro,
            List<Viagem> viagens
    ) {
        LocalDate hoje = LocalDate.now(RegrasReserva.FUSO);

        var elegiveis = viagens.stream()
                .filter(v -> RegrasReserva.impedimento(v, 0, hoje) == null)
                .filter(v -> !passageiros.existsByViagem_IdAndPassageiro_Id(
                        v.getId(),
                        passageiro.getId()
                ))
                .map(v -> new Candidata(
                        v,
                        passageiros.countByViagem_Id(v.getId())
                ))
                .filter(c -> RegrasReserva.impedimento(
                        c.viagem(),
                        c.ocupacao(),
                        hoje
                ) == null)
                .toList();

        if (elegiveis.isEmpty()) {
            return List.of();
        }

        var distancias = geografia.distanciasRodoviarias(passageiro.getId());

        return elegiveis.stream()
                .map(c -> {
                    long vagas = c.viagem().getVeiculo().getCapacidade()
                            - c.ocupacao();

                    Double km = distancias.get(c.viagem().getId());

                    double peso = km == null || !Double.isFinite(km) || km < 0
                            ? Double.MAX_VALUE
                            : km / (1.0 + vagas);

                    return new ArestaPassageiroVeiculo(
                            passageiro.getId(),
                            c.viagem(),
                            peso
                    );
                })
                .sorted(
                        Comparator.comparingDouble(ArestaPassageiroVeiculo::peso)
                                .thenComparing(a -> a.viagem().getDataViagem())
                                .thenComparing(a -> a.viagem().getHorarioSaidaPrevisto())
                                .thenComparing(a -> a.viagem().getCodigoViagem())
                )
                .toList();
    }

    private record Candidata(Viagem viagem, long ocupacao) {
    }
}