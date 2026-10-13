package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.MetodoDistancia;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.rota.repository.RotaRepository;
import com.VanControl.VanControl.viagem.domain.dto.request.ConsultarRecomendacoesRequestDto;
import com.VanControl.VanControl.viagem.domain.dto.response.PaginaRecomendacoesResponseDto;
import com.VanControl.VanControl.viagem.domain.dto.response.RecomendacaoViagemResponseDto;
import com.VanControl.VanControl.viagem.repository.RecomendacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecomendacaoService {

    private final RecomendacaoRepository recomendacoes;
    private final PassageiroRepository passageiros;
    private final RotaRepository rotas;
    private final SecurityUtils security;

    @Transactional(readOnly = true)
    public PaginaRecomendacoesResponseDto buscar(
            ConsultarRecomendacoesRequestDto filtro,
            int page,
            int size
    ) {
        LocalDate hoje = LocalDate.now(RegrasReserva.FUSO);
        filtro.validar(hoje);

        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException(
                    "page deve ser >= 0 e size deve estar entre 1 e 100"
            );
        }

        var usuario = security.getAuthenticatedUser();

        var passageiro = passageiros.findByUser_Id(usuario.getId())
                .orElseThrow(
                        () -> new NotFoundException(
                                "Passageiro não encontrado"
                        )
                );

        var rota = rotas.findByCodigoRota(filtro.codigoRota().trim())
                .orElseThrow(
                        () -> new NotFoundException("Rota inexistente")
                );

        if (rota.getDestino() == null || rota.getDestino().isBlank()) {
            throw new ConflictException(
                    "Rota sem destino definido; revise o cadastro"
            );
        }

        var ordenadas = recomendacoes.buscar(
                        passageiro.getId(),
                        rota.getId(),
                        filtro,
                        hoje
                )
                .stream()
                .map(this::pontuar)
                .sorted(
                        Comparator.comparing(
                                        RecomendacaoViagemResponseDto::pontuacao,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                                .thenComparing(
                                        RecomendacaoViagemResponseDto::chegadaPrevista
                                )
                                .thenComparing(
                                        r -> r.viagem().horarioSaidaPrevisto()
                                )
                                .thenComparing(
                                        r -> r.viagem().codigoViagem()
                                )
                )
                .toList();

        long inicio = (long) page * size;

        var conteudo = inicio >= ordenadas.size()
                ? List.<RecomendacaoViagemResponseDto>of()
                : ordenadas.subList(
                (int) inicio,
                (int) Math.min(
                        inicio + size,
                        ordenadas.size()
                )
        );

        return new PaginaRecomendacoesResponseDto(
                conteudo,
                page,
                size,
                ordenadas.size(),
                (int) ((ordenadas.size() + (long) size - 1) / size)
        );
    }

    private RecomendacaoViagemResponseDto pontuar(
            RecomendacaoViagemResponseDto r
    ) {
        var d = r.distancia();
        Double km = d.distanciaKm();

        Double peso = "CALCULADA".equals(d.estado())
                && d.metodo() == MetodoDistancia.RODOVIARIA
                && km != null
                && Double.isFinite(km)
                && km >= 0
                ? km / (1.0 + r.vagasDisponiveis())
                : null;

        return new RecomendacaoViagemResponseDto(
                r.viagem(),
                r.destino(),
                r.chegadaPrevista(),
                r.vagasDisponiveis(),
                d,
                peso
        );
    }
}