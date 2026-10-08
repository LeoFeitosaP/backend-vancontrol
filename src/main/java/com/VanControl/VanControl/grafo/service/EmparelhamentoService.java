package com.VanControl.VanControl.grafo.service;

import com.VanControl.VanControl.grafo.domain.entity.ArestaPassageiroVeiculo;
import com.VanControl.VanControl.grafo.domain.entity.PassageiroVeiculoDistancia;
import com.VanControl.VanControl.grafo.repository.PassageiroVeiculoDistanciaRepository;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.veiculo.repository.VeiculoRepository;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmparelhamentoService {

    private final PassageiroRepository passageiroRepository;
    private final VeiculoRepository veiculoRepository;
    private final ViagemPassageiroRepository viagemPassageiroRepository;
    private final PassageiroVeiculoDistanciaRepository distanciaRepository;
    private final GeoLocalizacaoService geoLocalizacaoService;

    public List<ArestaPassageiroVeiculo> recomendarViagensParaPassageiro(
            Passageiro passageiro,
            List<Viagem> viagens
    ) {
        return viagens.stream()
                .map(viagem -> {
                    var veiculo = viagem.getVeiculo();

                    if (veiculo == null) {
                        return null;
                    }

                    long ocupacao =
                            viagemPassageiroRepository.countByViagem_Id(
                                    viagem.getId()
                            );

                    int vagasLivres =
                            veiculo.getCapacidade() - (int) ocupacao;

                    double distancia =
                            obterOuCalcularDistancia(passageiro, veiculo);

                    double peso = vagasLivres <= 0
                            ? Double.MAX_VALUE
                            : distancia / (1 + vagasLivres);

                    return new ArestaPassageiroVeiculo(
                            passageiro.getId(),
                            viagem,
                            peso
                    );
                })
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparingDouble(
                                ArestaPassageiroVeiculo::peso
                        )
                )
                .toList();
    }

    private double obterOuCalcularDistancia(
            Passageiro passageiro,
            Veiculo veiculo
    ) {
        return distanciaRepository
                .findByPassageiro_IdAndVeiculo_idVeiculo(
                        passageiro.getId(),
                        veiculo.getIdVeiculo()
                )
                .map(PassageiroVeiculoDistancia::getDistanciaKm)
                .orElseGet(
                        () -> calcularEArmazenar(passageiro, veiculo)
                );
    }

    private double calcularEArmazenar(
            Passageiro passageiro,
            Veiculo veiculo
    ) {
        garantirCoordenadas(passageiro);
        garantirCoordenadasVeiculo(veiculo);

        double km = geoLocalizacaoService.calcularDistanciaKm(
                passageiro.getLatitude(),
                passageiro.getLongitude(),
                veiculo.getLatitude(),
                veiculo.getLongitude()
        );

        distanciaRepository.save(
                PassageiroVeiculoDistancia.builder()
                        .passageiro(passageiro)
                        .veiculo(veiculo)
                        .distanciaKm(km)
                        .calculadoEm(LocalDateTime.now())
                        .build()
        );

        return km;
    }

    private void garantirCoordenadas(Passageiro passageiro) {
        if (passageiro.getLatitude() == null) {
            var coordenadas = geoLocalizacaoService.geocodificar(
                    passageiro.getEndereco(),
                    passageiro.getCep()
            );

            passageiro.setLatitude(coordenadas.latitude());
            passageiro.setLongitude(coordenadas.longitude());
            passageiroRepository.save(passageiro);
        }
    }

    private void garantirCoordenadasVeiculo(Veiculo veiculo) {
        if (veiculo.getLatitude() == null) {
            var coordenadas = geoLocalizacaoService.geocodificar(
                    veiculo.getEnderecoInicial(),
                    ""
            );

            veiculo.setLatitude(coordenadas.latitude());
            veiculo.setLongitude(coordenadas.longitude());
            veiculoRepository.save(veiculo);
        }
    }
}