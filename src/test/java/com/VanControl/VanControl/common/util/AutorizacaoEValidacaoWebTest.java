package com.VanControl.VanControl.common.util;

import com.VanControl.VanControl.common.service.CredentialsService;
import com.VanControl.VanControl.common.exception.GlobalExceptionHandler;
import com.VanControl.VanControl.common.exception.model.NotFoundException;
import com.VanControl.VanControl.common.security.CustomAccessDeniedHandler;
import com.VanControl.VanControl.common.security.CustomAuthenticationEntryPoint;
import com.VanControl.VanControl.common.security.CustomUserDetailsService;
import com.VanControl.VanControl.common.security.SecurityConfig;
import com.VanControl.VanControl.common.security.SecurityFilter;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.common.util.SecurityUtils;
import com.VanControl.VanControl.motorista.controller.MotoristaController;
import com.VanControl.VanControl.motorista.domain.dto.request.CadastrarMotoristaRequestDto;
import com.VanControl.VanControl.motorista.domain.dto.response.MotoristaDefaultResponseDto;
import com.VanControl.VanControl.motorista.service.MotoristaService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.user.controller.AuthController;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private static final String CODIGO_VIAGEM = "VIA-12345678";
    private static final String CPF_PASSAGEIRO = "02245419006";
    private static final String CPF_OUTRO = "52998224725";

    private static final String REGISTRO_VALIDO = """
            {
              "name": "Joao Silva",
              "email": "joao@example.com",
              "password": "Senha123",
              "cpf": "02245419006",
              "telefone": "(15) 99999-1234",
              "instituicaoEnsino": "UNISO",
              "turno": "Noite",
              "endereco": "Rua Exemplo, 123, Centro, Sorocaba, SP",
              "cep": "18000-000"
            }
            """;

    private static final String MOTORISTA_VALIDO = """
            {
              "name": "Maria Santos",
              "email": "maria@example.com",
              "cnh": "98765432101",
              "categoriaCnh": "D",
              "dataValidadeCnh": "2099/12",
              "cpf": "52998224725",
              "telefone": "(15) 98888-1234",
              "password": "Senha123"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void passageiroPodeRemoverPropriaAssociacao() throws Exception {
        when(viagemService.removerPassageiro(
                CODIGO_VIAGEM,
                CPF_PASSAGEIRO
        )).thenReturn(
                new ViagemDefaultResponseDto(
                        "Passageiro removido da viagem."
                )
        );

        mockMvc.perform(
                        delete(
                                "/viagens/{codigo}/passageiros/{cpf}",
                                CODIGO_VIAGEM,
                                CPF_PASSAGEIRO
                        ).with(usuario(Role.PASSAGEIRO, CPF_PASSAGEIRO))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem")
                        .value("Passageiro removido da viagem."));

        verify(viagemService).removerPassageiro(
                CODIGO_VIAGEM,
                CPF_PASSAGEIRO
        );
    }

    @Test
    void passageiroNaoPodeRemoverOutraPessoa() throws Exception {
        mockMvc.perform(
                        delete(
                                "/viagens/{codigo}/passageiros/{cpf}",
                                CODIGO_VIAGEM,
                                CPF_OUTRO
                        ).with(usuario(Role.PASSAGEIRO, CPF_PASSAGEIRO))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(viagemService);
    }

    @Test
    void remocaoExigeAutenticacao() throws Exception {
        mockMvc.perform(
                        delete(
                                "/viagens/{codigo}/passageiros/{cpf}",
                                CODIGO_VIAGEM,
                                CPF_PASSAGEIRO
                        )
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(viagemService);
    }

    @Test
    void administradorPodeRemoverPassageiro() throws Exception {
        when(viagemService.removerPassageiro(
                CODIGO_VIAGEM,
                CPF_PASSAGEIRO
        )).thenReturn(
                new ViagemDefaultResponseDto(
                        "Passageiro removido da viagem."
                )
        );

        mockMvc.perform(
                        delete(
                                "/viagens/{codigo}/passageiros/{cpf}",
                                CODIGO_VIAGEM,
                                CPF_PASSAGEIRO
                        ).with(usuario(Role.ADMIN, null))
                )
                .andExpect(status().isOk());

        verify(viagemService).removerPassageiro(
                CODIGO_VIAGEM,
                CPF_PASSAGEIRO
        );
    }

    @Test
    void motoristaMantemPermissaoPrevistaNaPoliticaAtual() throws Exception {
        when(viagemService.removerPassageiro(
                CODIGO_VIAGEM,
                CPF_PASSAGEIRO
        )).thenReturn(
                new ViagemDefaultResponseDto(
                        "Passageiro removido da viagem."
                )
        );

        mockMvc.perform(
                        delete(
                                "/viagens/{codigo}/passageiros/{cpf}",
                                CODIGO_VIAGEM,
                                CPF_PASSAGEIRO
                        ).with(usuario(Role.MOTORISTA, CPF_OUTRO))
                )
                .andExpect(status().isOk());

        verify(viagemService).removerPassageiro(
                CODIGO_VIAGEM,
                CPF_PASSAGEIRO
        );
    }

    @Test
    void associacaoInexistenteRetorna404() throws Exception {
        when(viagemService.removerPassageiro(
                CODIGO_VIAGEM,
                CPF_PASSAGEIRO
        )).thenThrow(new NotFoundException("Associação não encontrada"));

        mockMvc.perform(
                        delete(
                                "/viagens/{codigo}/passageiros/{cpf}",
                                CODIGO_VIAGEM,
                                CPF_PASSAGEIRO
                        ).with(usuario(Role.PASSAGEIRO, CPF_PASSAGEIRO))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem")
                        .value("Associação não encontrada"));
    }

    @Test
    void passageiroContinuaImpedidoDeConcluirViagem() throws Exception {
        mockMvc.perform(
                        put("/viagens/{codigo}", CODIGO_VIAGEM)
                                .with(usuario(Role.PASSAGEIRO, CPF_PASSAGEIRO))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(viagemService);
    }

    @Test
    void registroValidoContinuaPublicoERetorna201() throws Exception {
        when(credentialsService.registrarUsuario(
                any(RegisterRequestDTO.class)
        )).thenReturn(new ResponseDTO("Joao Silva", "token-de-teste"));

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REGISTRO_VALIDO)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Joao Silva"))
                .andExpect(jsonPath("$.token").value("token-de-teste"));

        verify(credentialsService).registrarUsuario(
                any(RegisterRequestDTO.class)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "email, joao@example.com, email-invalido",
            "cpf, 02245419006, 11111111111",
            "cep, 18000-000, 18000000",
            "password, Senha123, curta",
            "turno, Noite, Madrugada"
    })
    void registroInvalidoNaoChegaAoServico(
            String campo,
            String valorOriginal,
            String valorInvalido
    ) throws Exception {
        String corpo = REGISTRO_VALIDO.replace(
                valorOriginal,
                valorInvalido
        );

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value(containsString(campo + ":")));

        verifyNoInteractions(credentialsService);
    }

    @Test
    void registroSemNomeRetorna400() throws Exception {
        String corpo = REGISTRO_VALIDO.replace(
                "\"name\": \"Joao Silva\",",
                ""
        );

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("name: Insira seu nome completo"));

        verifyNoInteractions(credentialsService);
    }

    @Test
    void motoristaNaoExigeCamposExclusivosDePassageiro() throws Exception {
        when(motoristaService.cadastrarMotorista(
                any(CadastrarMotoristaRequestDto.class)
        )).thenReturn(
                new MotoristaDefaultResponseDto(
                        "Motorista cadastrado com sucesso"
                )
        );

        mockMvc.perform(
                        post("/motoristas")
                                .with(usuario(Role.ADMIN, null))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(MOTORISTA_VALIDO)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Motorista cadastrado com sucesso"));

        verify(motoristaService).cadastrarMotorista(
                any(CadastrarMotoristaRequestDto.class)
        );

        verifyNoInteractions(credentialsService);
    }

    @Test
    void passageiroNaoPodeCadastrarMotorista() throws Exception {
        mockMvc.perform(
                        post("/motoristas")
                                .with(usuario(Role.PASSAGEIRO, CPF_PASSAGEIRO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(MOTORISTA_VALIDO)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(motoristaService, credentialsService);
    }

    private static RequestPostProcessor usuario(Role role, String cpf) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Usuario Teste");
        user.setEmail("usuario@example.com");
        user.setCpf(cpf);
        user.setRole(role);

        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );

        var autenticacao = new UsernamePasswordAuthenticationToken(
                user,
                null,
                authorities
        );

        return authentication(autenticacao);
    }
}
