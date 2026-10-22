package com.VanControl.VanControl.passageiro.controller;

import com.VanControl.VanControl.common.exception.GlobalExceptionHandler;
import com.VanControl.VanControl.common.security.*;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.passageiro.domain.dto.response.PassageiroResponseDto;
import com.VanControl.VanControl.passageiro.service.PassageiroService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemResumoResponseDto;
import com.VanControl.VanControl.viagem.service.ViagemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PassageiroController.class)
@Import({
        SecurityConfig.class,
        SecurityFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        CustomUserDetailsService.class,
        SecurityUtils.class,
        GlobalExceptionHandler.class
})
class PassageiroControllerTest {

    private static final String CPF = "02245419006";
    private static final String OUTRO = "52998224725";

    private static final String CORPO = """
            {"nome":"Joao Silva","turno":"Tarde",
             "endereco":{"cep":"01001000","numero":"200"}}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PassageiroService passageiroService;

    @MockitoBean
    private ViagemService viagemService;

    @ParameterizedTest
    @EnumSource(Role.class)
    void consultaMantemPoliticaDoController(Role role) throws Exception {
        when(passageiroService.buscarPassageiroPorCpf(CPF))
                .thenReturn(resposta());

        mvc.perform(
                        get("/passageiros/{cpf}", CPF)
                                .with(usuario(role))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF));
    }

    @Test
    void passageiroNaoConsultaOutroCpf() throws Exception {
        mvc.perform(
                        get("/passageiros/{cpf}", OUTRO)
                                .with(usuario(Role.PASSAGEIRO))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(passageiroService);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "PASSAGEIRO"})
    void atualizaEnderecoEstruturado(Role role) throws Exception {
        when(passageiroService.atualizarPassageiro(eq(CPF), any()))
                .thenReturn(resposta());

        mvc.perform(
                        put("/passageiros/{cpf}", CPF)
                                .with(usuario(role))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CORPO)
                )
                .andExpect(status().isOk());

        verify(passageiroService).atualizarPassageiro(
                eq(CPF),
                argThat(dto ->
                        dto.endereco() != null
                                && "200".equals(dto.endereco().numero())
                )
        );
    }

    @Test
    void passageiroNaoAtualizaOutroCpf() throws Exception {
        mvc.perform(
                        put("/passageiros/{cpf}", OUTRO)
                                .with(usuario(Role.PASSAGEIRO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CORPO)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(passageiroService);
    }

    @Test
    void enderecoSemNumeroRetorna400() throws Exception {
        mvc.perform(
                        put("/passageiros/{cpf}", CPF)
                                .with(usuario(Role.PASSAGEIRO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        CORPO.replace(",\"numero\":\"200\"", "")
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passageiroService);
    }

    @Test
    void listaViagensDoProprioPassageiro() throws Exception {
        var viagem = new ViagemResumoResponseDto(
                "VIA-12345678",
                "ROT-123",
                "ABC-1234",
                LocalDate.of(2026, 5, 10),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                false
        );

        when(viagemService.listarViagensPorPassageiroCpf(eq(CPF), any()))
                .thenReturn(new PageImpl<>(List.of(viagem)));

        mvc.perform(
                        get("/passageiros/{cpf}/viagens", CPF)
                                .with(usuario(Role.PASSAGEIRO))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].codigoViagem")
                                .value("VIA-12345678")
                );
    }

    private PassageiroResponseDto resposta() {
        return new PassageiroResponseDto(
                "Joao Silva",
                CPF,
                "(11) 99999-1234",
                "joao@example.com",
                "UNISO",
                "Tarde",
                "Endereço legado",
                "01001-000"
        );
    }

    private RequestPostProcessor usuario(Role role) {
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setCpf(CPF);
        user.setRole(role);

        return authentication(
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_" + role.name())
                        )
                )
        );
    }
}