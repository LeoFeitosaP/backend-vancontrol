package com.VanControl.VanControl.viagem.controller;

import com.VanControl.VanControl.common.exception.GlobalExceptionHandler;
import com.VanControl.VanControl.common.exception.model.BadRequestException;
import com.VanControl.VanControl.common.security.*;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.viagem.domain.dto.response.PaginaRecomendacoesResponseDto;
import com.VanControl.VanControl.viagem.service.RecomendacaoService;
import com.VanControl.VanControl.viagem.service.ViagemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        RecomendacaoController.class,
        ViagemController.class
})
@Import({
        SecurityConfig.class,
        SecurityFilter.class,
        CustomAccessDeniedHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomUserDetailsService.class,
        GlobalExceptionHandler.class
})
class RecomendacaoWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecomendacaoService service;

    @MockitoBean
    private ViagemService viagens;

    @MockitoBean
    private SecurityUtils security;

    @MockitoBean
    private TokenService tokens;

    @MockitoBean
    private UserRepository users;

    @Test
    void rotaLiteralNaoEhInterpretadaComoCodigoDeViagem() throws Exception {
        when(service.buscar(any(), eq(0), eq(10)))
                .thenReturn(
                        new PaginaRecomendacoesResponseDto(
                                List.of(), 0, 10, 0, 0
                        )
                );

        mvc.perform(
                        consulta().with(
                                user("passageiro").roles("PASSAGEIRO")
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verifyNoInteractions(viagens);
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(consulta())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "MOTORISTA"})
    void buscaPessoalExigePerfilPassageiro(String role) throws Exception {
        mvc.perform(
                        consulta().with(user("usuario").roles(role))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void exigeParametros() throws Exception {
        mvc.perform(
                        get("/viagens/recomendacoes")
                                .with(
                                        user("passageiro")
                                                .roles("PASSAGEIRO")
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rejeitaDataMalformada() throws Exception {
        mvc.perform(
                        get("/viagens/recomendacoes")
                                .param("codigoRota", "ROT-TESTE")
                                .param("dataViagem", "invalida")
                                .param(
                                        "chegadaDe",
                                        "2099-10-10T18:30:00"
                                )
                                .param(
                                        "chegadaAte",
                                        "2099-10-10T19:30:00"
                                )
                                .with(
                                        user("passageiro")
                                                .roles("PASSAGEIRO")
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void erroDeRegraMantemContratoMensagem() throws Exception {
        when(service.buscar(any(), anyInt(), anyInt()))
                .thenThrow(
                        new BadRequestException("Janela inválida")
                );

        mvc.perform(
                        consulta().with(
                                user("passageiro").roles("PASSAGEIRO")
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.mensagem").value("Janela inválida")
                );
    }

    private MockHttpServletRequestBuilder consulta() {
        return get("/viagens/recomendacoes")
                .param("codigoRota", "ROT-TESTE")
                .param("dataViagem", "2099-10-10")
                .param("chegadaDe", "2099-10-10T18:30:00")
                .param("chegadaAte", "2099-10-10T19:30:00");
    }
}