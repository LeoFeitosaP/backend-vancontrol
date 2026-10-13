package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.veiculo.repository.VeiculoRepository;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagem.repository.ViagemRepository;
import com.VanControl.VanControl.viagemPassageiro.domain.entity.ViagemPassageiro;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViagemServiceTest {

    @Mock
    private ViagemRepository viagemRepository;

    @Mock
    private ViagemPassageiroRepository viagemPassageiroRepository;

    @Mock
    private PassageiroRepository passageiroRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @InjectMocks
    private ViagemService viagemService;

    private Viagem viagem;
    private Passageiro passageiro;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        viagem = new Viagem();
        viagem.setId(UUID.randomUUID());
        viagem.setCodigoViagem("VIA-1234");
        viagem.setPlacaVeiculo("ABC-1234");
        viagem.setDataViagem(LocalDate.now(RegrasReserva.FUSO).plusDays(5));
        viagem.setViagemConcluida(false);
        viagem.setHorarioSaidaPrevisto(LocalTime.of(18, 0));
        viagem.setHorarioChegadaPrevisto(LocalTime.of(19, 0));

        User user = new User();
        user.setCpf("52998224725");
        user.setName("João");

        passageiro = new Passageiro();
        passageiro.setId(UUID.randomUUID());
        passageiro.setUser(user);

        veiculo = new Veiculo(
                "ABC-1234",
                "M.Benz",
                "Van X",
                2020,
                15,
                "12345678901",
                "DISPONIVEL"
        );

        veiculo.setIdVeiculo(UUID.randomUUID());
        viagem.setVeiculo(veiculo);
    }

    @Test
    @DisplayName("Deve adicionar passageiro com sucesso")
    void deveAdicionarPassageiroComSucesso() {
        when(veiculoRepository.buscarParaAtualizacao(veiculo.getIdVeiculo()))
                .thenReturn(veiculo);

        when(viagemRepository.buscarParaAtualizacao(viagem.getCodigoViagem()))
                .thenReturn(viagem);

        when(passageiroRepository.findByUser_Cpf(passageiro.getUser().getCpf()))
                .thenReturn(Optional.of(passageiro));

        when(viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(
                viagem.getId(), passageiro.getId()
        )).thenReturn(false);

        when(viagemPassageiroRepository.countByViagem_Id(viagem.getId()))
                .thenReturn(5L);

        var resposta = viagemService.adicionarPassageiro(
                viagem.getCodigoViagem(),
                passageiro.getUser().getCpf()
        );

        assertEquals("Passageiro associado à viagem.", resposta.mensagem());

        var ordem = inOrder(
                viagemRepository,
                veiculoRepository,
                viagemPassageiroRepository
        );

        ordem.verify(viagemRepository)
                .buscarParaAtualizacao(viagem.getCodigoViagem());

        ordem.verify(veiculoRepository)
                .buscarParaAtualizacao(veiculo.getIdVeiculo());

        ordem.verify(viagemPassageiroRepository)
                .existsByViagem_IdAndPassageiro_Id(
                        viagem.getId(),
                        passageiro.getId()
                );

        ordem.verify(viagemPassageiroRepository)
                .countByViagem_Id(viagem.getId());

        ordem.verify(viagemPassageiroRepository)
                .saveAndFlush(any(ViagemPassageiro.class));
    }

    @Test
    @DisplayName("Lança NotFoundException ao tentar adicionar em viagem inexistente")
    void deveLancarExceptionViagemInexistente() {
        when(viagemRepository.buscarParaAtualizacao("VIA-INEX"))
                .thenReturn(null);

        assertThrows(
                NotFoundException.class,
                () -> viagemService.adicionarPassageiro(
                        "VIA-INEX",
                        passageiro.getUser().getCpf()
                )
        );
    }

    @Test
    @DisplayName("Lança BadRequestException se viagem concluída")
    void deveLancarExceptionSeViagemConcluida() {
        when(veiculoRepository.buscarParaAtualizacao(veiculo.getIdVeiculo()))
                .thenReturn(veiculo);

        viagem.setViagemConcluida(true);

        when(viagemRepository.buscarParaAtualizacao(viagem.getCodigoViagem()))
                .thenReturn(viagem);

        when(passageiroRepository.findByUser_Cpf(passageiro.getUser().getCpf()))
                .thenReturn(Optional.of(passageiro));

        assertThrows(
                BadRequestException.class,
                () -> viagemService.adicionarPassageiro(
                        viagem.getCodigoViagem(),
                        passageiro.getUser().getCpf()
                )
        );
    }

    @Test
    @DisplayName("Lança ConflictException se passageiro já associado")
    void deveLancarExceptionSePassageiroJaAssociado() {
        when(veiculoRepository.buscarParaAtualizacao(veiculo.getIdVeiculo()))
                .thenReturn(veiculo);

        when(viagemRepository.buscarParaAtualizacao(viagem.getCodigoViagem()))
                .thenReturn(viagem);

        when(passageiroRepository.findByUser_Cpf(passageiro.getUser().getCpf()))
                .thenReturn(Optional.of(passageiro));

        when(viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(
                viagem.getId(), passageiro.getId()
        )).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> viagemService.adicionarPassageiro(
                        viagem.getCodigoViagem(),
                        passageiro.getUser().getCpf()
                )
        );
    }

    @Test
    @DisplayName("Lança BadRequestException se estourar capacidade")
    void deveLancarExceptionSeCapacidadeMax() {
        when(veiculoRepository.buscarParaAtualizacao(veiculo.getIdVeiculo()))
                .thenReturn(veiculo);

        viagem.setDataViagem(LocalDate.now(RegrasReserva.FUSO).plusDays(2));

        when(viagemRepository.buscarParaAtualizacao(viagem.getCodigoViagem()))
                .thenReturn(viagem);

        when(passageiroRepository.findByUser_Cpf(passageiro.getUser().getCpf()))
                .thenReturn(Optional.of(passageiro));

        when(viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(
                viagem.getId(), passageiro.getId()
        )).thenReturn(false);

        when(viagemPassageiroRepository.countByViagem_Id(viagem.getId()))
                .thenReturn(15L);

        assertThrows(
                BadRequestException.class,
                () -> viagemService.adicionarPassageiro(
                        viagem.getCodigoViagem(),
                        passageiro.getUser().getCpf()
                )
        );
    }

    @Test
    @DisplayName("Deve remover passageiro com sucesso")
    void deveRemoverPassageiroComSucesso() {
        when(viagemRepository.buscarParaAtualizacao(viagem.getCodigoViagem()))
                .thenReturn(viagem);

        when(passageiroRepository.findByUser_Cpf(passageiro.getUser().getCpf()))
                .thenReturn(Optional.of(passageiro));

        when(viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(
                viagem.getId(), passageiro.getId()
        )).thenReturn(true);

        var resposta = viagemService.removerPassageiro(
                viagem.getCodigoViagem(),
                passageiro.getUser().getCpf()
        );

        assertEquals("Passageiro removido da viagem.", resposta.mensagem());

        verify(viagemPassageiroRepository)
                .deleteByViagem_IdAndPassageiro_Id(
                        viagem.getId(),
                        passageiro.getId()
                );
    }

    @Test
    @DisplayName("Lança NotFoundException se associação não existe ao remover")
    void deveLancarExcecaoAoRemoverSeNaoAssociado() {
        when(viagemRepository.buscarParaAtualizacao(viagem.getCodigoViagem()))
                .thenReturn(viagem);

        when(passageiroRepository.findByUser_Cpf(passageiro.getUser().getCpf()))
                .thenReturn(Optional.of(passageiro));

        when(viagemPassageiroRepository.existsByViagem_IdAndPassageiro_Id(
                viagem.getId(), passageiro.getId()
        )).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> viagemService.removerPassageiro(
                        viagem.getCodigoViagem(),
                        passageiro.getUser().getCpf()
                )
        );
    }

    @Test
    @DisplayName("Deve listar passageiros de uma viagem")
    void deveListarPassageirosPorViagem() {
        when(viagemRepository.findByCodigoViagem(viagem.getCodigoViagem()))
                .thenReturn(viagem);

        ViagemPassageiro vp = new ViagemPassageiro();
        vp.setPassageiro(passageiro);

        when(viagemPassageiroRepository.findByViagem_Id(viagem.getId()))
                .thenReturn(List.of(vp));

        when(viagemPassageiroRepository.countByViagem_Id(viagem.getId()))
                .thenReturn(1L);

        var resposta = viagemService.listarPassageirosPorViagem(
                viagem.getCodigoViagem()
        );

        assertEquals(viagem.getCodigoViagem(), resposta.codigoViagem());
        assertEquals(1, resposta.ocupacao());
        assertEquals(1, resposta.passageiros().size());

        assertEquals(
                passageiro.getUser().getCpf(),
                resposta.passageiros().getFirst().cpf()
        );
    }

    @Test
    @DisplayName("Deve listar viagens de um passageiro")
    void deveListarViagensPorPassageiro() {
        when(passageiroRepository.findByUser_Cpf(passageiro.getUser().getCpf()))
                .thenReturn(Optional.of(passageiro));

        ViagemPassageiro vp = new ViagemPassageiro();
        vp.setViagem(viagem);

        when(viagemPassageiroRepository.findByPassageiro_Id(
                eq(passageiro.getId()),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(vp))
        );

        org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(0, 10);

        var resposta = viagemService.listarViagensPorPassageiroCpf(
                passageiro.getUser().getCpf(),
                pageRequest
        );

        assertEquals(1, resposta.getTotalElements());

        assertEquals(
                viagem.getCodigoViagem(),
                resposta.getContent().get(0).codigoViagem()
        );
    }
}