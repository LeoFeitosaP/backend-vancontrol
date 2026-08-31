package com.VanControl.VanControl.viagemPassageiro.domain.entity;

import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "viagem_passageiro",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"viagem_id", "passageiro_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViagemPassageiro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", nullable = false)
    private Viagem viagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passageiro_id", nullable = false)
    private Passageiro passageiro;

    private LocalDateTime dataAssociacao;
}
