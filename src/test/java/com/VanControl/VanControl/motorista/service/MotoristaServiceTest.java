package com.VanControl.VanControl.motorista.service;

import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.motorista.domain.dto.request.AtualizarTelefoneMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.request.CadastrarMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaDefaultResponseDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaResponseDto;
import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.motorista.repository.MotoristaRepository;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.common.Service.CredentialsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotoristaServiceTest {

    @Mock
    private MotoristaRepository motoristaRepository;

    @Mock
    private CredentialsService credentialsService;

    @InjectMocks
    private MotoristaService motoristaService;

    private Motorista motorista;
    private CadastrarMotoristaRequestDto cadastrarRequestDto;
    private AtualizarTelefoneMotoristaRequestDto atualizarTelefoneRequestDto;

    private final String CPF_VALIDO = "02245419006";
    private final String CPF_INEXISTENTE = "12345678901";
    private final String CNH_VALIDA = "98765432101";
    private final String TELEFONE_VALIDO = "(11) 11111-1111";
    private final String TELEFONE_NOVO = "(11) 22222-2222";

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Maria Santos");
        user.setCpf(CPF_VALIDO);

        motorista = new Motorista();
        motorista.setId(UUID.randomUUID());
        motorista.setUser(user);
        motorista.setCnh(CNH_VALIDA);
        motorista.setCategoriaCnh("D");
        motorista.setDataValidadeCnh(YearMonth.of(2026, 12));
        motorista.setTelefone(TELEFONE_VALIDO);

        cadastrarRequestDto = new CadastrarMotoristaRequestDto(
                "Maria Santos",
                "maria.santos@example.com",
                CNH_VALIDA,
                "D",
                YearMonth.of(2026, 12),
                CPF_VALIDO,
                TELEFONE_VALIDO,
                "Senha123"
        );

        atualizarTelefoneRequestDto = new AtualizarTelefoneMotoristaRequestDto(
                CPF_VALIDO,
                TELEFONE_NOVO
        );
    }

    // ========== Testes de cadastrarMotorista ==========

    @Test
    @DisplayName("Deve cadastrar motorista com sucesso")
    void deveCadastrarMotoristaComSucesso() {
        // CORREÇÃO: Optional.empty() ao invés de null
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.empty());
        when(credentialsService.registrarUsuario(any(RegisterRequestDTO.class))).thenReturn(null);
        when(motoristaRepository.save(any(Motorista.class))).thenReturn(motorista);

        MotoristaDefaultResponseDto resposta = motoristaService.cadastrarMotorista(cadastrarRequestDto);

        assertEquals("Motorista cadastrado com sucesso", resposta.message());
        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_VALIDO);
        verify(credentialsService, times(1)).registrarUsuario(any(RegisterRequestDTO.class));
        verify(motoristaRepository, times(1)).save(any(Motorista.class));
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao cadastrar motorista com CPF já existente")
    void deveLancarExceptionAoCadastrarMotoristaComCpfExistente() {
        // CORREÇÃO: Optional.of()
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.of(motorista));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> motoristaService.cadastrarMotorista(cadastrarRequestDto));

        assertEquals("Motorista já cadastrado", exception.getMessage());
        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_VALIDO);
        verify(credentialsService, never()).registrarUsuario(any());
        verify(motoristaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve chamar userService ao cadastrar motorista")
    void deveChamarUserServiceAoCadastrarMotorista() {
        // CORREÇÃO: Optional.empty()
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.empty());
        when(credentialsService.registrarUsuario(any(RegisterRequestDTO.class))).thenReturn(null);
        when(motoristaRepository.save(any(Motorista.class))).thenReturn(motorista);

        motoristaService.cadastrarMotorista(cadastrarRequestDto);

        verify(credentialsService, times(1)).registrarUsuario(any(RegisterRequestDTO.class));
    }

    // ========== Testes de buscarMotoristaPorCpf ==========

    @Test
    @DisplayName("Deve buscar motorista por CPF com sucesso")
    void deveBuscarMotoristaPorCpfComSucesso() {
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.of(motorista));

        MotoristaResponseDto resposta = motoristaService.buscarMotoristaPorCpf(CPF_VALIDO);

        assertNotNull(resposta);
        assertEquals(motorista.getUser().getName(), resposta.nome());
        assertEquals(motorista.getUser().getCpf(), resposta.cpf());
        assertEquals(motorista.getCnh(), resposta.cnh());
        assertEquals(motorista.getCategoriaCnh(), resposta.categoriaCnh());
        assertEquals(motorista.getTelefone(), resposta.telefone());
        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_VALIDO);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar motorista inexistente")
    void deveLancarExceptionAoBuscarMotoristaPorCpfInexistente() {
        // CORREÇÃO: Optional.empty()
        when(motoristaRepository.findByUser_Cpf(CPF_INEXISTENTE)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> motoristaService.buscarMotoristaPorCpf(CPF_INEXISTENTE));

        assertEquals("Motorista não encontrado", exception.getMessage());
        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_INEXISTENTE);
    }

    // ========== Testes de buscarTodosMotoristas ==========

    @Test
    @DisplayName("Deve listar todos os motoristas com sucesso")
    void deveListarTodosMotoristasComSucesso() {
        User user2 = new User();
        user2.setName("João Silva");
        user2.setCpf("52837429031");

        Motorista motorista2 = new Motorista();
        motorista2.setId(UUID.randomUUID());
        motorista2.setUser(user2);
        motorista2.setCnh("11122233344");
        motorista2.setCategoriaCnh("B");
        motorista2.setDataValidadeCnh(YearMonth.of(2027, 6));
        motorista2.setTelefone("(21) 99999-9999");

        when(motoristaRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(motorista, motorista2)));

        Pageable pageable = PageRequest.of(0, 10);
        Page<MotoristaResponseDto> resposta = motoristaService.buscarTodosMotoristas(pageable);

        assertNotNull(resposta);
        assertEquals(2, resposta.getTotalElements());
        assertEquals("Maria Santos", resposta.getContent().get(0).nome());
        assertEquals("João Silva", resposta.getContent().get(1).nome());

        verify(motoristaRepository, times(1)).findAll(any(org.springframework.data.domain.PageRequest.class));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há motoristas cadastrados")
    void deveRetornarListaVaziaQuandoNaoHaMotoristas() {
        when(motoristaRepository.findAll(any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        Pageable pageable = PageRequest.of(0, 10);
        Page<MotoristaResponseDto> resposta = motoristaService.buscarTodosMotoristas(pageable);

        assertNotNull(resposta);
        assertTrue(resposta.isEmpty());
        verify(motoristaRepository, times(1)).findAll(any(org.springframework.data.domain.PageRequest.class));
    }

    // ========== Testes de atualizarTelefoneMotorista ==========

    @Test
    @DisplayName("Deve atualizar telefone do motorista com sucesso")
    void deveAtualizarTelefoneMotoristaComSucesso() {
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.of(motorista));
        when(motoristaRepository.save(any(Motorista.class))).thenReturn(motorista);

        MotoristaDefaultResponseDto resposta = motoristaService.atualizarTelefoneMotorista(atualizarTelefoneRequestDto);

        assertEquals("Telefone do motorista atualizado com sucesso", resposta.message());
        assertEquals(TELEFONE_NOVO, motorista.getTelefone());
        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_VALIDO);
        verify(motoristaRepository, times(1)).save(motorista);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao atualizar telefone de motorista inexistente")
    void deveLancarExceptionAoAtualizarTelefoneDeMotoristaInexistente() {
        AtualizarTelefoneMotoristaRequestDto requestInexistente = new AtualizarTelefoneMotoristaRequestDto(
                CPF_INEXISTENTE,
                TELEFONE_NOVO
        );
        // CORREÇÃO: Optional.empty()
        when(motoristaRepository.findByUser_Cpf(CPF_INEXISTENTE)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> motoristaService.atualizarTelefoneMotorista(requestInexistente));

        assertEquals("Motorista não encontrado", exception.getMessage());
        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_INEXISTENTE);
        verify(motoristaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve persistir a alteração de telefone no banco de dados")
    void devePersistirAlteracaoDeTelefone() {
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.of(motorista));
        when(motoristaRepository.save(any(Motorista.class))).thenReturn(motorista);

        motoristaService.atualizarTelefoneMotorista(atualizarTelefoneRequestDto);

        verify(motoristaRepository, times(1)).save(motorista);
    }

    // ========== Testes de deletarMotorista ==========

    @Test
    @DisplayName("Deve deletar motorista com sucesso")
    void deveDeletarMotoristaComSucesso() {
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.of(motorista));
        doNothing().when(motoristaRepository).delete(motorista);

        MotoristaDefaultResponseDto resposta = motoristaService.deletarMotorista(CPF_VALIDO);

        assertEquals("Motorista deletado com sucesso", resposta.message());
        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_VALIDO);
        verify(motoristaRepository, times(1)).delete(motorista);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao deletar motorista inexistente")
    void deveLancarExceptionAoDeletarMotoristaInexistente() {
        // CORREÇÃO: Optional.empty()
        when(motoristaRepository.findByUser_Cpf(CPF_INEXISTENTE)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> motoristaService.deletarMotorista(CPF_INEXISTENTE));

        assertEquals("Motorista não encontrado", exception.getMessage());
        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_INEXISTENTE);
        verify(motoristaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve chamar delete no repository ao deletar motorista")
    void deveChamarDeleteNoRepositoryAoDeletarMotorista() {
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.of(motorista));
        doNothing().when(motoristaRepository).delete(motorista);

        motoristaService.deletarMotorista(CPF_VALIDO);

        verify(motoristaRepository, times(1)).delete(motorista);
    }

    // ========== Testes do método interno buscarMotoristaPorCpfInterno ==========

    @Test
    @DisplayName("Deve utilizar método interno para buscar motorista ao atualizar telefone")
    void deveUtilizarMetodoInternoParaBuscarMotoristaNaAtualizacao() {
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.of(motorista));
        when(motoristaRepository.save(any(Motorista.class))).thenReturn(motorista);

        motoristaService.atualizarTelefoneMotorista(atualizarTelefoneRequestDto);

        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_VALIDO);
    }

    @Test
    @DisplayName("Deve utilizar método interno para buscar motorista ao deletar")
    void deveUtilizarMetodoInternoParaBuscarMotoristaAoDeletar() {
        when(motoristaRepository.findByUser_Cpf(CPF_VALIDO)).thenReturn(Optional.of(motorista));
        doNothing().when(motoristaRepository).delete(motorista);

        motoristaService.deletarMotorista(CPF_VALIDO);

        verify(motoristaRepository, times(1)).findByUser_Cpf(CPF_VALIDO);
    }
}