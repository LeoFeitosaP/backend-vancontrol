package com.VanControl.VanControl.viagem.service;

import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.grafo.service.EmparelhamentoService;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaResponseDto;
import com.VanControl.VanControl.motorista.service.MotoristaService;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.rota.service.RotaService;
import com.VanControl.VanControl.veiculo.service.VeiculoService;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViagemCadastroEOrdenacaoTest {

    @Mock
    private ViagemRepository viagemRepository;

    @Mock
    private ViagemPassageiroRepository viagemPassageiroRepository;

    @Mock
    private PassageiroRepository passageiroRepository;

    @Mock
    private EmparelhamentoService emparelhamentoService;

    @Mock
    private RotaService rotaService;

    @Mock
    private VeiculoService veiculoService;

    @Mock
    private MotoristaService motoristaService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ViagemService viagemService;

    private static final String CPF = "52998224725";

    @Test
    void cadastroPersisteCpfDoMotoristaEMapperDevolveCpf() {
        var motorista = new MotoristaResponseDto(
                "Maria Santos",
                CPF,
                "maria@example.com",
                "98765432101",
                "D",
                YearMonth.of(2099, 12),
                "(15) 98888-1234"
        );

        when(motoristaService.buscarMotoristaPorCpf(CPF))
                .thenReturn(motorista);

        var resposta = viagemService.cadastrarViagem(request());

        var captor = ArgumentCaptor.forClass(Viagem.class);
        verify(viagemRepository).save(captor.capture());

        Viagem salva = captor.getValue();

        assertEquals(CPF, salva.getDocumentoMotorista());
        assertNotEquals(motorista.nome(), salva.getDocumentoMotorista());
        assertEquals(
                CPF,
                ViagemMapper.converterParaViagemDto(salva).cpfMotorista()
        );
        assertEquals("Viagem cadastrada.", resposta.mensagem());
        assertTrue(salva.getCodigoViagem().matches("VIA-[0-9A-F]{8}"));

        verify(rotaService).buscarRotaPorCodigo("ROT-TESTE");
        verify(veiculoService).buscarVeiculoPorPlaca("ABC-1D23");
    }

    @Test
    void motoristaInexistenteNaoPermiteGravarViagem() {
        when(motoristaService.buscarMotoristaPorCpf(CPF))
                .thenThrow(new NotFoundException("Motorista não encontrado"));

        assertThrows(
                NotFoundException.class,
                () -> viagemService.cadastrarViagem(request())
        );

        verify(viagemRepository, never()).save(any());
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
                () -> viagemService.listarViagensPorPassageiroCpf(CPF, pageable)
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