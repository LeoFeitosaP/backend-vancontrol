package com.VanControl.VanControl.motorista.controller;

import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.motorista.domain.dto.request.AtualizarTelefoneMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaDefaultResponseDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaResponseDto;
import com.VanControl.VanControl.motorista.service.MotoristaService;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MotoristaController.class)
@DisplayName("MotoristaController - Testes de validação de acesso por CPF")
class MotoristaControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private MotoristaService motoristaService;

    @MockitoBean
    private SecurityUtils securityUtils;

    private MotoristaResponseDto motoristaResponseDto;
    private AtualizarTelefoneMotoristaRequestDto atualizarTelefoneRequestDto;


    private final String CPF_PRINCIPAL = "02245419006";
    private final String CPF_OUTRO = "52837429031";

    @BeforeEach
    void setUp() {
        motoristaResponseDto = new MotoristaResponseDto(
                "Maria Santos",
                CPF_PRINCIPAL,
                "email@gmail.com",
                "98765432101",
                "D",
                YearMonth.of(2026, 12),
                "(11) 11111-1111"
        );

        atualizarTelefoneRequestDto = new AtualizarTelefoneMotoristaRequestDto(
                CPF_PRINCIPAL,
                "(11) 22222-2222"
        );
    }

    @Test
    @DisplayName("Deve buscar motorista por CPF quando MOTORISTA acessa seus próprios dados")
    @WithMockUser(username = CPF_PRINCIPAL, roles = "MOTORISTA")
    void deveBuscarMotoristaPorCpfQuandoMotoristaAcessaProprioDados() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_PRINCIPAL);
        when(motoristaService.buscarMotoristaPorCpf(CPF_PRINCIPAL)).thenReturn(motoristaResponseDto);

        mockMvc.perform(get("/motoristas/{cpf}", CPF_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_PRINCIPAL))
                .andExpect(jsonPath("$.nome").value("Maria Santos"));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PRINCIPAL);
        verify(motoristaService, times(1)).buscarMotoristaPorCpf(CPF_PRINCIPAL);
    }

    @Test
    @DisplayName("Deve permitir MOTORISTA buscar dados de outro motorista")
    @WithMockUser(username = CPF_PRINCIPAL, roles = "MOTORISTA")
    void devePermitirMotoristaBuscarDadosDeOutroMotorista() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_OUTRO);
        when(motoristaService.buscarMotoristaPorCpf(CPF_OUTRO)).thenReturn(motoristaResponseDto);

        mockMvc.perform(get("/motoristas/{cpf}", CPF_OUTRO))
                .andExpect(status().isOk());

        verify(securityUtils, times(1)).validateCpfAccess(CPF_OUTRO);
        verify(motoristaService, times(1)).buscarMotoristaPorCpf(CPF_OUTRO);
    }

    @Test
    @DisplayName("Deve bloquear PASSAGEIRO de buscar dados de motorista")
    @WithMockUser(username = CPF_OUTRO, roles = "PASSAGEIRO")
    void deveBloquerPassageiroDeBuscarDadosDeMotorista() throws Exception {
        doThrow(new AccessDeniedException("Você não tem permissão para acessar dados de outro usuário"))
                .when(securityUtils).validateCpfAccess(CPF_PRINCIPAL);

        mockMvc.perform(get("/motoristas/{cpf}", CPF_PRINCIPAL))
                .andExpect(status().isForbidden());

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PRINCIPAL);
        verify(motoristaService, never()).buscarMotoristaPorCpf(any());
    }

    @Test
    @DisplayName("Deve permitir ADMIN buscar motorista por qualquer CPF")
    @WithMockUser(username = "80506044065", roles = "ADMIN")
    void devePermitirAdminBuscarMotoristaPorQualquerCpf() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_PRINCIPAL);
        when(motoristaService.buscarMotoristaPorCpf(CPF_PRINCIPAL)).thenReturn(motoristaResponseDto);

        mockMvc.perform(get("/motoristas/{cpf}", CPF_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_PRINCIPAL));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PRINCIPAL);
        verify(motoristaService, times(1)).buscarMotoristaPorCpf(CPF_PRINCIPAL);
    }

    @Test
    @DisplayName("Deve atualizar telefone quando MOTORISTA atualiza seus próprios dados")
    @WithMockUser(username = CPF_PRINCIPAL, roles = "MOTORISTA")
    void deveAtualizarTelefoneQuandoMotoristaAtualizaProprioDados() throws Exception {
        MotoristaDefaultResponseDto responseDto = new MotoristaDefaultResponseDto("Telefone atualizado com sucesso");
        doNothing().when(securityUtils).validateCpfAccess(CPF_PRINCIPAL);
        when(motoristaService.atualizarTelefoneMotorista(any(AtualizarTelefoneMotoristaRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/motoristas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizarTelefoneRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Telefone atualizado com sucesso"));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PRINCIPAL);
        verify(motoristaService, times(1)).atualizarTelefoneMotorista(any(AtualizarTelefoneMotoristaRequestDto.class));
    }

    @Test
    @DisplayName("Deve bloquear MOTORISTA de atualizar telefone de outro motorista")
    @WithMockUser(username = CPF_PRINCIPAL, roles = "MOTORISTA")
    void deveBloquerMotoristaDeAtualizarTelefoneDeOutroMotorista() throws Exception {
        AtualizarTelefoneMotoristaRequestDto requestOutroCpf = new AtualizarTelefoneMotoristaRequestDto(
                CPF_OUTRO,
                "(11)22222-2222"
        );

        doThrow(new AccessDeniedException("Você não tem permissão para acessar dados de outro usuário"))
                .when(securityUtils).validateCpfAccess(CPF_OUTRO);

        mockMvc.perform(put("/motoristas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOutroCpf)))
                .andExpect(status().isForbidden());

        verify(securityUtils, times(1)).validateCpfAccess(CPF_OUTRO);
        verify(motoristaService, never()).atualizarTelefoneMotorista(any());
    }

    @Test
    @DisplayName("Deve permitir ADMIN atualizar telefone de qualquer motorista")
    @WithMockUser(username = "80506044065", roles = "ADMIN")
    void devePermitirAdminAtualizarTelefoneDeQualquerMotorista() throws Exception {
        MotoristaDefaultResponseDto responseDto = new MotoristaDefaultResponseDto("Telefone atualizado com sucesso");
        doNothing().when(securityUtils).validateCpfAccess(CPF_PRINCIPAL);
        when(motoristaService.atualizarTelefoneMotorista(any(AtualizarTelefoneMotoristaRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/motoristas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizarTelefoneRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Telefone atualizado com sucesso"));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PRINCIPAL);
        verify(motoristaService, times(1)).atualizarTelefoneMotorista(any(AtualizarTelefoneMotoristaRequestDto.class));
    }

    @Test
    @DisplayName("Deve bloquear PASSAGEIRO de atualizar telefone de motorista")
    @WithMockUser(username = "09363065096", roles = "PASSAGEIRO")
    void deveBloquerPassageiroDeAtualizarTelefoneDeMotorista() throws Exception {
        doThrow(new AccessDeniedException("Você não tem permissão para acessar dados de outro usuário"))
                .when(securityUtils).validateCpfAccess(CPF_PRINCIPAL);

        mockMvc.perform(put("/motoristas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizarTelefoneRequestDto)))
                .andExpect(status().isForbidden());

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PRINCIPAL);
        verify(motoristaService, never()).atualizarTelefoneMotorista(any());
    }
}