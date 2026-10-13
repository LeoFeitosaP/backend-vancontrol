package com.VanControl.VanControl.grafo;

import com.VanControl.VanControl.common.exception.GlobalExceptionHandler;
import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.security.*;
import com.VanControl.VanControl.grafo.controller.GeografiaPersistidaController;
import com.VanControl.VanControl.grafo.domain.dto.GeografiaPersistida.Tipo;
import com.VanControl.VanControl.grafo.service.GeoLocalizacaoService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GeografiaPersistidaController.class)
@Import({
        SecurityConfig.class,
        SecurityFilter.class,
        CustomAccessDeniedHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomUserDetailsService.class,
        GlobalExceptionHandler.class
})
class GeografiaPersistidaWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GeoLocalizacaoService service;

    @MockitoBean
    private TokenService tokens;

    @MockitoBean
    private UserRepository users;

    @Test
    void exigeLogin() throws Exception {
        mvc.perform(
                        get(
                                "/geolocalizacao/cadastros/PASSAGEIRO/52998224725"
                        )
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void passageiroNaoProcessaLocalizacaoAlheia() throws Exception {
        mvc.perform(
                        post(
                                "/geolocalizacao/cadastros/PASSAGEIRO/52998224725/processar"
                        )
                                .with(
                                        user("passageiro")
                                                .roles("PASSAGEIRO")
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void conflitoDeRevisaoRetorna409() throws Exception {
        when(
                service.processar(
                        Tipo.PASSAGEIRO,
                        "52998224725",
                        false
                )
        ).thenThrow(
                new ConflictException("Endereço mudou")
        );

        mvc.perform(
                        post(
                                "/geolocalizacao/cadastros/PASSAGEIRO/52998224725/processar"
                        )
                                .with(
                                        user("admin").roles("ADMIN")
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.mensagem")
                                .value("Endereço mudou")
                );
    }

    @Test
    void revisaoDeOrigemExigeJustificativa() throws Exception {
        mvc.perform(
                        put(
                                "/geolocalizacao/cadastros/viagens/VIA-TESTE/origem"
                        )
                                .with(
                                        user("admin").roles("ADMIN")
                                )
                                .contentType("application/json")
                                .content(
                                        "{\"endereco\":{\"cep\":\"01001000\",\"numero\":\"100\"}}"
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}