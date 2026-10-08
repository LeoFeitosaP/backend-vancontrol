package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.rota.domain.entity.Rota;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagem.mapper.ViagemMapper;
import com.VanControl.VanControl.viagem.repository.ViagemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ViagemRelacionamentosJpaTest {

    @Autowired private EntityManager em;
    @Autowired private ViagemRepository viagens;

    private Rota rota;
    private Veiculo veiculo;
    private Motorista motorista;

    enum Vinculo {
        ROTA,
        VEICULO,
        MOTORISTA
    }

    @BeforeEach
    void preparar() {
        rota = new Rota();
        rota.setCodigoRota("ROT-TESTE");
        em.persist(rota);

        veiculo = new Veiculo(
                "ABC-1D23",
                "Mercedes",
                "Sprinter",
                2024,
                15,
                "12345678901",
                "DISPONIVEL"
        );

        em.persist(veiculo);

        var user = new User();
        user.setCpf("529.982.247-25");
        user.setRole(Role.MOTORISTA);
        em.persist(user);

        motorista = new Motorista();
        motorista.setUser(user);
        em.persist(motorista);
    }

    @Test
    void persisteAsTresChavesEstrangeiras() {
        viagens.saveAndFlush(novaViagem());
        em.clear();

        var salva = viagens.findByCodigoViagem("VIA-TESTE");

        assertEquals(
                rota.getId(),
                salva.getRota().getId()
        );

        assertEquals(
                veiculo.getIdVeiculo(),
                salva.getVeiculo().getIdVeiculo()
        );

        assertEquals(
                motorista.getId(),
                salva.getMotorista().getId()
        );

        assertEquals(
                "52998224725",
                ViagemMapper.converterParaViagemDto(salva).cpfMotorista()
        );
    }

    @Test
    void alteracaoDoCadastroNaoReescreveIdentificadoresHistoricos() {
        viagens.saveAndFlush(novaViagem());

        veiculo.setPlaca("XYZ-9A87");
        rota.setCodigoRota("ROT-NOVA");
        motorista.getUser().setCpf("11144477735");

        em.flush();
        em.clear();

        var salva = viagens.findByCodigoViagem("VIA-TESTE");
        var dto = ViagemMapper.converterParaViagemDto(salva);

        assertEquals("ABC-1D23", dto.placaVeiculo());
        assertEquals("ROT-TESTE", dto.codigoRota());
        assertEquals("52998224725", dto.cpfMotorista());
        assertEquals("XYZ-9A87", salva.getVeiculo().getPlaca());
    }

    @ParameterizedTest
    @EnumSource(Vinculo.class)
    void entidadeReferenciadaNaoPodeSerExcluida(Vinculo vinculo) {
        viagens.saveAndFlush(novaViagem());
        em.clear();

        Object entidade = switch (vinculo) {
            case ROTA -> em.find(Rota.class, rota.getId());

            case VEICULO -> em.find(
                    Veiculo.class,
                    veiculo.getIdVeiculo()
            );

            case MOTORISTA -> em.find(
                    Motorista.class,
                    motorista.getId()
            );
        };

        assertThrows(PersistenceException.class, () -> {
            em.remove(entidade);
            em.flush();
        });
    }

    @Test
    void viagemNaoAceitaMotoristaAusente() {
        var viagem = novaViagem();
        viagem.setMotorista(null);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> viagens.saveAndFlush(viagem)
        );
    }

    private Viagem novaViagem() {
        var viagem = new Viagem(
                rota,
                veiculo,
                motorista,
                LocalDate.of(2099, 10, 10),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0)
        );

        viagem.setCodigoViagem("VIA-TESTE");
        return viagem;
    }
}