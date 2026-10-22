package com.VanControl.VanControl.common.util;

import com.VanControl.VanControl.common.service.CredentialsService;
import com.VanControl.VanControl.common.exception.GlobalExceptionHandler;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.security.*;
import com.VanControl.VanControl.motorista.controller.MotoristaController;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaDefaultResponseDto;
import com.VanControl.VanControl.motorista.service.MotoristaService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.user.controller.AuthController;
import com.VanControl.VanControl.user.domain.dto.response.ResponseDTO;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.viagem.controller.ViagemController;
import com.VanControl.VanControl.viagem.domain.dto.response.ViagemDefaultResponseDto;
import com.VanControl.VanControl.viagem.service.ViagemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AuthController.class,
        ViagemController.class,
        MotoristaController.class
})
@Import({
        SecurityConfig.class,
        SecurityFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        CustomUserDetailsService.class,
        SecurityUtils.class,
        GlobalExceptionHandler.class
})
class AutorizacaoEValidacaoWebTest {

    private static final String VIAGEM = "VIA-12345678";
    private static final String CPF = "02245419006";
    private static final String OUTRO = "52998224725";

    private static final String REGISTRO = """
            {"name":"Joao Silva","email":"joao@example.com","password":"Senha123",
             "cpf":"02245419006","telefone":"(15) 99999-1234",
             "instituicaoEnsino":"UNISO","turno":"Noite",
             "endereco":{"cep":"01001-000","numero":"123"}}
            """;

    private static final String MOTORISTA = """
            {"name":"Maria Santos","email":"maria@example.com","cnh":"98765432101",
             "categoriaCnh":"D","dataValidadeCnh":"2099/12","cpf":"52998224725",
             "telefone":"(15) 98888-1234","password":"Senha123"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CredentialsService credentialsService;

    @MockitoBean
    private ViagemService viagemService;

    @MockitoBean
    private MotoristaService motoristaService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @ParameterizedTest
    @CsvSource({
            "PASSAGEIRO,02245419006",
            "ADMIN,52998224725",
            "MOTORISTA,52998224725"
    })
    void papeisAutorizadosRemovemAssociacao(Role role, String cpf) throws Exception {
        when(viagemService.removerPassageiro(VIAGEM, CPF))
                .thenReturn(
                        new ViagemDefaultResponseDto("Passageiro removido da viagem.")
                );

        mvc.perform(
                        delete("/viagens/{codigo}/passageiros/{cpf}", VIAGEM, CPF)
                                .with(usuario(role, cpf))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.mensagem").value("Passageiro removido da viagem.")
                );

        verify(viagemService).removerPassageiro(VIAGEM, CPF);
    }

    @Test
    void passageiroNaoRemoveOutraPessoa() throws Exception {
        mvc.perform(
                        delete("/viagens/{codigo}/passageiros/{cpf}", VIAGEM, OUTRO)
                                .with(usuario(Role.PASSAGEIRO, CPF))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(viagemService);
    }

    @Test
    void remocaoExigeAutenticacao() throws Exception {
        mvc.perform(
                        delete("/viagens/{codigo}/passageiros/{cpf}", VIAGEM, CPF)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(viagemService);
    }

    @Test
    void associacaoInexistenteRetorna404() throws Exception {
        when(viagemService.removerPassageiro(VIAGEM, CPF))
                .thenThrow(new NotFoundException("Associação não encontrada"));

        mvc.perform(
                        delete("/viagens/{codigo}/passageiros/{cpf}", VIAGEM, CPF)
                                .with(usuario(Role.PASSAGEIRO, CPF))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.mensagem").value("Associação não encontrada")
                );
    }

    @Test
    void passageiroNaoConcluiViagem() throws Exception {
        mvc.perform(
                        put("/viagens/{codigo}", VIAGEM)
                                .with(usuario(Role.PASSAGEIRO, CPF))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(viagemService);
    }

    @Test
    void registroEstruturadoContinuaPublico() throws Exception {
        when(credentialsService.registrarUsuario(any()))
                .thenReturn(new ResponseDTO("Joao Silva", "token"));

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REGISTRO)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token"));

        verify(credentialsService).registrarUsuario(any());
    }

    @ParameterizedTest
    @CsvSource({
            "email,joao@example.com,email-invalido",
            "cpf,02245419006,11111111111",
            "endereco.cep,01001-000,123",
            "password,Senha123,curta",
            "turno,Noite,Madrugada",
            "endereco.numero,123,''"
    })
    void registroInvalidoNaoChegaAoServico(
            String campo,
            String original,
            String invalido
    ) throws Exception {
        String corpo = REGISTRO.replace(
                "\"" + original + "\"",
                "\"" + invalido + "\""
        );

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(containsString(campo + ":"))
                );

        verifyNoInteractions(credentialsService);
    }

    @Test
    void registroSemNomeRetorna400() throws Exception {
        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        REGISTRO.replace(
                                                "\"name\":\"Joao Silva\",",
                                                ""
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.mensagem")
                                .value("name: Insira seu nome completo")
                );

        verifyNoInteractions(credentialsService);
    }

    @Test
    void registroSemEnderecoRetorna400() throws Exception {
        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        REGISTRO.replace(
                                                "{\"cep\":\"01001-000\",\"numero\":\"123\"}",
                                                "null"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.mensagem")
                                .value("endereco: Informe o endereço")
                );

        verifyNoInteractions(credentialsService);
    }

    @Test
    void motoristaNaoExigeEnderecoNemCamposDePassageiro() throws Exception {
        when(motoristaService.cadastrarMotorista(any()))
                .thenReturn(
                        new MotoristaDefaultResponseDto(
                                "Motorista cadastrado com sucesso"
                        )
                );

        mvc.perform(
                        post("/motoristas")
                                .with(usuario(Role.ADMIN, null))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(MOTORISTA)
                )
                .andExpect(status().isCreated());

        verify(motoristaService).cadastrarMotorista(any());
        verifyNoInteractions(credentialsService);
    }

    @Test
    void passageiroNaoCadastraMotorista() throws Exception {
        mvc.perform(
                        post("/motoristas")
                                .with(usuario(Role.PASSAGEIRO, CPF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(MOTORISTA)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(motoristaService, credentialsService);
    }

    private static RequestPostProcessor usuario(Role role, String cpf) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("usuario@example.com");
        user.setCpf(cpf);
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