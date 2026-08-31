package com.VanControl.VanControl.passageiro.domain.entity;

import com.VanControl.VanControl.pagamento.domain.entity.Pagamento;
import com.VanControl.VanControl.viagemPassageiro.domain.entity.ViagemPassageiro;
import com.VanControl.VanControl.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passageiro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @OneToMany(mappedBy = "passageiro")
    @Builder.Default
    private List<Pagamento> pagamentos = new ArrayList<>();

    @OneToMany(mappedBy = "passageiro")
    @Builder.Default
    private List<ViagemPassageiro> viagens = new ArrayList<>();

    @Column(unique = true)
    private String telefone;
    private String instituicaoEnsino;
    private String turno;
    private String endereco;
    private String cep;

    public Passageiro(String telefone, String intituicaoEnsino, String turno,
                      String endereco, String cep) {
        this.telefone = telefone;
        this.instituicaoEnsino = intituicaoEnsino;
        this.turno = turno;
        this.endereco = endereco;
        this.cep = cep;
    }
}
