package com.VanControl.VanControl.motorista.service;

import com.VanControl.VanControl.common.service.CredentialsService;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.motorista.domain.dto.request.AtualizarTelefoneMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.request.CadastrarMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.motorista.repository.MotoristaRepository;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotoristaServiceTest {

    @Mock
    private MotoristaRepository motoristaRepository;

    @Mock
    private CredentialsService credentialsService;

    @InjectMocks
    private MotoristaService motoristaService;

    private static final String CPF = "52998224725";

    private Motorista motorista;
    private CadastrarMotoristaRequestDto request;

    @BeforeEach
    void preparar() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Maria Santos");
        user.setEmail("maria@example.com");
        user.setCpf(CPF);
        user.setRole(Role.MOTORISTA);

        motorista = new Motorista(
                "98765432101",
                "D",
                YearMonth.of(2099, 12),
                "(15) 98888-1234"
        );
        motorista.setId(UUID.randomUUID());
        motorista.setUser(user);

        request = new CadastrarMotoristaRequestDto(
                "Maria Santos",
                "maria@example.com",
                "98765432101",
                "D",
                YearMonth.of(2099, 12),
                CPF,
                "(15) 98888-1234",
                "Senha123"
        );
    }

    @Test
    void cadastroAssociaExatamenteOUsuarioRetornado() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.empty());

        when(credentialsService.criarUsuarioMotorista(any()))
                .thenReturn(motorista.getUser());

        assertEquals(
                "Motorista cadastrado com sucesso",
                motoristaService.cadastrarMotorista(request).message()
        );

        var captor = ArgumentCaptor.forClass(Motorista.class);
        verify(motoristaRepository).save(captor.capture());

        assertSame(
                motorista.getUser(),
                captor.getValue().getUser()
        );
        assertEquals(
                Role.MOTORISTA,
                captor.getValue().getUser().getRole()
        );

        var dtoCaptor = ArgumentCaptor.forClass(RegisterRequestDTO.class);
        verify(credentialsService).criarUsuarioMotorista(dtoCaptor.capture());

        assertEquals(CPF, dtoCaptor.getValue().cpf());
        assertNull(dtoCaptor.getValue().instituicaoEnsino());

        verify(credentialsService, never()).registrarUsuario(any());
    }

    @Test
    void motoristaJaCadastradoNaoCriaOutroUsuario() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.of(motorista));

        assertThrows(
                ConflictException.class,
                () -> motoristaService.cadastrarMotorista(request)
        );

        verifyNoInteractions(credentialsService);
        verify(motoristaRepository, never()).save(any());
    }

    @Test
    void conflitoDeIdentidadeNaoGravaMotorista() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.empty());

        when(credentialsService.criarUsuarioMotorista(any()))
                .thenThrow(new ConflictException("CPF já cadastrado"));

        assertThrows(
                ConflictException.class,
                () -> motoristaService.cadastrarMotorista(request)
        );

        verify(motoristaRepository, never()).save(any());
    }

    @Test
    void buscaDevolveDadosDoUsuarioAssociado() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.of(motorista));

        var resposta = motoristaService.buscarMotoristaPorCpf(CPF);

        assertEquals(CPF, resposta.cpf());
        assertEquals("Maria Santos", resposta.nome());
        assertEquals("maria@example.com", resposta.email());
        assertEquals(motorista.getCnh(), resposta.cnh());
    }

    @Test
    void buscaInexistenteRetornaNotFound() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> motoristaService.buscarMotoristaPorCpf(CPF)
        );
    }

    @Test
    void listaMotoristasEVazio() {
        var pagina = PageRequest.of(0, 10);

        when(motoristaRepository.findAll(pagina))
                .thenReturn(
                        new PageImpl<>(List.of(motorista)),
                        Page.empty(pagina)
                );

        assertEquals(
                CPF,
                motoristaService.buscarTodosMotoristas(pagina)
                        .getContent()
                        .getFirst()
                        .cpf()
        );

        assertTrue(
                motoristaService.buscarTodosMotoristas(pagina).isEmpty()
        );
    }

    @Test
    void atualizaTelefone() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.of(motorista));

        var dto = new AtualizarTelefoneMotoristaRequestDto(
                CPF,
                "(15) 97777-1234"
        );

        assertEquals(
                "Telefone do motorista atualizado com sucesso",
                motoristaService.atualizarTelefoneMotorista(dto).message()
        );

        assertEquals(dto.novoTelefone(), motorista.getTelefone());
        verify(motoristaRepository).save(motorista);
    }

    @Test
    void atualizarInexistenteNaoSalva() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> motoristaService.atualizarTelefoneMotorista(
                        new AtualizarTelefoneMotoristaRequestDto(
                                CPF,
                                "(15) 97777-1234"
                        )
                )
        );

        verify(motoristaRepository, never()).save(any());
    }

    @Test
    void excluiMotorista() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.of(motorista));

        assertEquals(
                "Motorista deletado com sucesso",
                motoristaService.deletarMotorista(CPF).message()
        );

        verify(motoristaRepository).delete(motorista);
    }

    @Test
    void excluirInexistenteNaoRemove() {
        when(motoristaRepository.findByUser_Cpf(CPF))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> motoristaService.deletarMotorista(CPF)
        );

        verify(motoristaRepository, never()).delete(any());
    }
}