package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.grafo.service.EmparelhamentoService;
import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.rota.domain.entity.Rota;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.motorista.repository.MotoristaRepository;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.rota.repository.RotaRepository;
import com.VanControl.VanControl.veiculo.repository.VeiculoRepository;
import com.VanControl.VanControl.viagem.domain.dto.request.CriarViagemRequestDto;
import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import com.VanControl.VanControl.viagem.mapper.ViagemMapper;
import com.VanControl.VanControl.viagem.repository.ViagemRepository;
import com.VanControl.VanControl.viagemPassageiro.repository.ViagemPassageiroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViagemCadastroEOrdenacaoTest {

    @Mock private ViagemRepository viagemRepository;
    @Mock private ViagemPassageiroRepository viagemPassageiroRepository;
    @Mock private PassageiroRepository passageiroRepository;
    @Mock private EmparelhamentoService emparelhamentoService;
    @Mock private RotaRepository rotaRepository;
    @Mock private VeiculoRepository veiculoRepository;
    @Mock private MotoristaRepository motoristaRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private ViagemService viagemService;

    private static final String CPF = "52998224725";

    @Test
    void cadastroPersisteVinculosECopiasHistoricas() {
        var rota = new Rota();
        rota.setCodigoRota("ROT-TESTE");

        var veiculo = new Veiculo();
        veiculo.setPlaca("ABC-1D23");

        var user = new User();
        user.setCpf(CPF);
        user.setName("Maria Santos");

        var motorista = new Motorista();
        motorista.setUser(user);

        when(rotaRepository.findByCodigoRota("ROT-TESTE"))
                .thenReturn(Optional.of(rota));

        when(veiculoRepository.findByPlaca("ABC-1D23"))
                .thenReturn(veiculo);

        when(motoristaRepository.findAllByCpfNormalizado(CPF))
                .thenReturn(List.of(motorista));

        var resposta = viagemService.cadastrarViagem(request());

        var captor = ArgumentCaptor.forClass(Viagem.class);
        verify(viagemRepository).saveAndFlush(captor.capture());

        var salva = captor.getValue();

        assertSame(rota, salva.getRota());
        assertSame(veiculo, salva.getVeiculo());
        assertSame(motorista, salva.getMotorista());
        assertEquals(CPF, salva.getDocumentoMotorista());

        assertEquals(
                CPF,
                ViagemMapper.converterParaViagemDto(salva).cpfMotorista()
        );

        assertEquals("Viagem cadastrada.", resposta.mensagem());
        assertTrue(salva.getCodigoViagem().matches("VIA-[0-9A-F]{8}"));
    }

    @Test
    void motoristaInexistenteNaoPermiteGravarViagem() {
        when(rotaRepository.findByCodigoRota("ROT-TESTE"))
                .thenReturn(Optional.of(new Rota()));

        when(veiculoRepository.findByPlaca("ABC-1D23"))
                .thenReturn(new Veiculo());

        when(motoristaRepository.findAllByCpfNormalizado(CPF))
                .thenReturn(List.of());

        assertThrows(
                NotFoundException.class,
                () -> viagemService.cadastrarViagem(request())
        );

        verify(viagemRepository, never()).saveAndFlush(any());
    }

    @Test
    void traduzOrdenacaoCompostaEPreservaMetadadosPublicos() {
        Passageiro passageiro = passageiro();

        when(passageiroRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.of(passageiro));

        when(viagemPassageiroRepository.findByPassageiro_Id(
                eq(passageiro.getId()),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Pageable solicitado = PageRequest.of(
                2,
                5,
                Sort.by(
                        Sort.Order.desc("dataViagem"),
                        Sort.Order.asc("horarioSaidaPrevisto")
                )
        );

        var resposta = viagemService.listarViagensPorPassageiroCpf(
                CPF,
                solicitado
        );

        var captor = ArgumentCaptor.forClass(Pageable.class);

        verify(viagemPassageiroRepository).findByPassageiro_Id(
                eq(passageiro.getId()),
                captor.capture()
        );

        Pageable consulta = captor.getValue();

        assertEquals(2, consulta.getPageNumber());
        assertEquals(5, consulta.getPageSize());

        assertEquals(
                Sort.by(
                        Sort.Order.desc("viagem.dataViagem"),
                        Sort.Order.asc("viagem.horarioSaidaPrevisto"),
                        Sort.Order.asc("id")
                ),
                consulta.getSort()
        );

        assertEquals(solicitado, resposta.getPageable());
    }

    @Test
    void ordenacaoInvalidaNaoChegaAoRepository() {
        when(passageiroRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.of(passageiro()));

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by("campoInexistente")
        );

        var erro = assertThrows(
                BadRequestException.class,
                () -> viagemService.listarViagensPorPassageiroCpf(
                        CPF,
                        pageable
                )
        );

        assertEquals(
                "Campo de ordenação inválido para viagens do passageiro: campoInexistente",
                erro.getMessage()
        );

        verifyNoInteractions(viagemPassageiroRepository);
    }

    @Test
    void passageiroInexistenteRetornaNotFound() {
        when(passageiroRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> viagemService.listarViagensPorPassageiroCpf(
                        CPF,
                        PageRequest.of(0, 10)
                )
        );

        verifyNoInteractions(viagemPassageiroRepository);
    }

    @Test
    void chamadaSemPaginacaoRecebeOrdenacaoPadraoSemConsultarNumeroDaPagina() {
        Passageiro passageiro = passageiro();

        when(passageiroRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.of(passageiro));

        when(viagemPassageiroRepository.findByPassageiro_Id(
                eq(passageiro.getId()),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        viagemService.listarViagensPorPassageiroCpf(
                CPF,
                Pageable.unpaged()
        );

        var captor = ArgumentCaptor.forClass(Pageable.class);

        verify(viagemPassageiroRepository).findByPassageiro_Id(
                eq(passageiro.getId()),
                captor.capture()
        );

        assertTrue(captor.getValue().isUnpaged());

        assertEquals(
                Sort.by(
                        Sort.Order.desc("viagem.dataViagem"),
                        Sort.Order.asc("id")
                ),
                captor.getValue().getSort()
        );
    }

    private Passageiro passageiro() {
        var passageiro = new Passageiro();
        passageiro.setId(UUID.randomUUID());
        return passageiro;
    }

    private CriarViagemRequestDto request() {
        return new CriarViagemRequestDto(
                "ROT-TESTE",
                "ABC-1D23",
                CPF,
                LocalDate.of(2099, 10, 10),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0)
        );
    }
}