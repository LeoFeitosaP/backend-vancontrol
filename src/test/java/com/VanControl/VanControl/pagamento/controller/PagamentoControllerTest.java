package com.VanControl.VanControl.pagamento.controller;

import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.pagamento.domain.dto.response.PagamentoResponseDto;
import com.VanControl.VanControl.pagamento.domain.enums.StatusPagamento;
import com.VanControl.VanControl.pagamento.service.PagamentoService;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagamentoController.class)
@DisplayName("PagamentoController - Testes de validação de acesso por CPF")
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PagamentoService pagamentoService;

    @MockitoBean
    private SecurityUtils securityUtils;

    private List<PagamentoResponseDto> pagamentosResponseDto;

    private final String CPF_PASSAGEIRO = "12345678901";

    @BeforeEach
    void setUp() {
        PagamentoResponseDto pagamento1 = new PagamentoResponseDto(
                "PAG-123",
                "João Silva",
                "01/2024",
                new BigDecimal("150.00"),
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 8),
                StatusPagamento.PAGO
        );

        PagamentoResponseDto pagamento2 = new PagamentoResponseDto(
                "PAG-124",
                "João Silva",
                "02/2024",
                new BigDecimal("150.00"),
                LocalDate.of(2024, 2, 10),
                null,
                StatusPagamento.PENDENTE
        );

        pagamentosResponseDto = Arrays.asList(pagamento1, pagamento2);
    }

    @Test
    @DisplayName("Deve buscar pagamentos quando PASSAGEIRO acessa seus próprios dados")
    @WithMockUser(username = "12345678901", roles = "PASSAGEIRO")
    void deveBuscarPagamentosQuandoPassageiroAcessaProprioDados() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_PASSAGEIRO);
        // CORREÇÃO: mock ajustado para receber "eq(CPF)" e "any(Pageable.class)", além de retornar PageImpl
        when(pagamentoService.buscarPagamentosDoPassageiroPorCpf(eq(CPF_PASSAGEIRO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(pagamentosResponseDto));

        // CORREÇÃO: Respostas do tipo Page() criam o array de dados dentro do objeto `content` no JSON retornado
        mockMvc.perform(get("/pagamentos/passageiro/{cpf}", CPF_PASSAGEIRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].nome").value("João Silva"))
                .andExpect(jsonPath("$.content[0].status").value("PAGO"))
                .andExpect(jsonPath("$.content[1].status").value("PENDENTE"));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PASSAGEIRO);
        verify(pagamentoService, times(1)).buscarPagamentosDoPassageiroPorCpf(eq(CPF_PASSAGEIRO), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve bloquear PASSAGEIRO de buscar pagamentos de outro passageiro")
    @WithMockUser(username = "12345678901", roles = "PASSAGEIRO")
    void deveBloquerPassageiroDeBuscarPagamentosDeOutro() throws Exception {
        String CPF_OUTRO_PASSAGEIRO = "98765432100";
        doThrow(new AccessDeniedException("Você não tem permissão para acessar dados de outro usuário"))
                .when(securityUtils).validateCpfAccess(CPF_OUTRO_PASSAGEIRO);

        mockMvc.perform(get("/pagamentos/passageiro/{cpf}", CPF_OUTRO_PASSAGEIRO))
                .andExpect(status().isForbidden());

        verify(securityUtils, times(1)).validateCpfAccess(CPF_OUTRO_PASSAGEIRO);
        verify(pagamentoService, never()).buscarPagamentosDoPassageiroPorCpf(any(), any());
    }

    @Test
    @DisplayName("Deve permitir ADMIN buscar pagamentos de qualquer passageiro")
    @WithMockUser(username = "99999999999", roles = "ADMIN")
    void devePermitirAdminBuscarPagamentosDeQualquerPassageiro() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_PASSAGEIRO);
        when(pagamentoService.buscarPagamentosDoPassageiroPorCpf(eq(CPF_PASSAGEIRO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(pagamentosResponseDto));

        mockMvc.perform(get("/pagamentos/passageiro/{cpf}", CPF_PASSAGEIRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PASSAGEIRO);
        verify(pagamentoService, times(1)).buscarPagamentosDoPassageiroPorCpf(eq(CPF_PASSAGEIRO), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve permitir MOTORISTA buscar pagamentos de qualquer passageiro")
    @WithMockUser(username = "88888888888", roles = "MOTORISTA")
    void devePermitirMotoristaBuscarPagamentosDeQualquerPassageiro() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_PASSAGEIRO);
        when(pagamentoService.buscarPagamentosDoPassageiroPorCpf(eq(CPF_PASSAGEIRO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(pagamentosResponseDto));

        mockMvc.perform(get("/pagamentos/passageiro/{cpf}", CPF_PASSAGEIRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PASSAGEIRO);
        verify(pagamentoService, times(1)).buscarPagamentosDoPassageiroPorCpf(eq(CPF_PASSAGEIRO), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando passageiro não tem pagamentos")
    @WithMockUser(username = "12345678901", roles = "PASSAGEIRO")
    void deveRetornarListaVaziaQuandoPassageiroNaoTemPagamentos() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_PASSAGEIRO);
        when(pagamentoService.buscarPagamentosDoPassageiroPorCpf(eq(CPF_PASSAGEIRO), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/pagamentos/passageiro/{cpf}", CPF_PASSAGEIRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PASSAGEIRO);
        verify(pagamentoService, times(1)).buscarPagamentosDoPassageiroPorCpf(eq(CPF_PASSAGEIRO), any(Pageable.class));
    }
}
