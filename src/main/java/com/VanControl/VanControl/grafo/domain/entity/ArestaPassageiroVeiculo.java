package com.VanControl.VanControl.grafo.domain.entity;

import com.VanControl.VanControl.viagem.domain.entity.Viagem;

import java.util.UUID;

public record ArestaPassageiroVeiculo(
        UUID passageiroId,
        Viagem viagem,
        double peso
) {}