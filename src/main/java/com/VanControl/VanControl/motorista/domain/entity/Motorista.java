package com.VanControl.VanControl.motorista.domain.entity;

import com.VanControl.VanControl.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.YearMonth;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Motorista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(unique = true)
    private String cnh;
    private String categoriaCnh;
    private YearMonth dataValidadeCnh;
    @Column(unique = true)
    private String telefone;

    public Motorista(String cnh, String categoriaCnh, YearMonth dataValidadeCnh, String telefone) {
        this.cnh = cnh;
        this.categoriaCnh = categoriaCnh;
        this.dataValidadeCnh = dataValidadeCnh;
        this.telefone = telefone;
    }
}
