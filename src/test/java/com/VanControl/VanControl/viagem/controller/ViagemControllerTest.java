package com.VanControl.VanControl.viagem.controller;

import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemDefaultResponseDto;
import com.VanControl.VanControl.viagemPassageiro.domain.dto.ViagemPassageirosResponseDto;
import com.VanControl.VanControl.viagem.service.ViagemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ViagemController.class)
@DisplayName("ViagemController - Testes de conexao viagem-passageiro")
class ViagemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ViagemService viagemService;

    @MockitoBean
    private SecurityUtils securityUtils;

    private static final String CODIGO_VIAGEM = "VIA-12345678";
    private static final String CPF_PASSAGEIRO = "12345678901";

    @Test
    @DisplayName("Deve associar passageiro quando acessa seus proprios dados")
    @WithMockUser(username = CPF_PASSAGEIRO, roles = "PASSAGEIRO")
    void deveAssociarPassageiroComSucesso() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_PASSAGEIRO);
        when(viagemService.adicionarPassageiro(CODIGO_VIAGEM, CPF_PASSAGEIRO))
                .thenReturn(new ViagemDefaultResponseDto("Passageiro associado à viagem."));

        mockMvc.perform(post("/viagens/{codigo}/passageiros/{cpf}", CODIGO_VIAGEM, CPF_PASSAGEIRO)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Passageiro associado à viagem."));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PASSAGEIRO);
        verify(viagemService, times(1)).adicionarPassageiro(CODIGO_VIAGEM, CPF_PASSAGEIRO);
    }

    @Test
    @DisplayName("Deve remover passageiro quando acessa seus proprios dados")
    @WithMockUser(username = CPF_PASSAGEIRO, roles = "PASSAGEIRO")
    void deveRemoverPassageiroComSucesso() throws Exception {
        doNothing().when(securityUtils).validateCpfAccess(CPF_PASSAGEIRO);
        when(viagemService.removerPassageiro(CODIGO_VIAGEM, CPF_PASSAGEIRO))
                .thenReturn(new ViagemDefaultResponseDto("Passageiro removido da viagem."));

        mockMvc.perform(delete("/viagens/{codigo}/passageiros/{cpf}", CODIGO_VIAGEM, CPF_PASSAGEIRO)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Passageiro removido da viagem."));

        verify(securityUtils, times(1)).validateCpfAccess(CPF_PASSAGEIRO);
        verify(viagemService, times(1)).removerPassageiro(CODIGO_VIAGEM, CPF_PASSAGEIRO);
    }

    @Test
    @DisplayName("Deve listar passageiros de uma viagem se for motorista")
    @WithMockUser(roles = "MOTORISTA")
    void deveListarPassageirosPorViagem() throws Exception {
        ViagemPassageirosResponseDto response = new ViagemPassageirosResponseDto(
                CODIGO_VIAGEM, 15, 1, List.of()
        );
        when(viagemService.listarPassageirosPorViagem(CODIGO_VIAGEM)).thenReturn(response);

        mockMvc.perform(get("/viagens/{codigo}/passageiros", CODIGO_VIAGEM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoViagem").value(CODIGO_VIAGEM))
                .andExpect(jsonPath("$.capacidade").value(15))
                .andExpect(jsonPath("$.ocupacao").value(1));

        verify(viagemService, times(1)).listarPassageirosPorViagem(CODIGO_VIAGEM);
    }
}

