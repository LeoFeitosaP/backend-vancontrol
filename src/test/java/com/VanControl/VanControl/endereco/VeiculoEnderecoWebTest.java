package com.VanControl.VanControl.endereco;

import com.VanControl.VanControl.common.exception.GlobalExceptionHandler;
import com.VanControl.VanControl.common.security.CustomAccessDeniedHandler;
import com.VanControl.VanControl.common.security.CustomAuthenticationEntryPoint;
import com.VanControl.VanControl.common.security.CustomUserDetailsService;
import com.VanControl.VanControl.common.security.SecurityConfig;
import com.VanControl.VanControl.common.security.SecurityFilter;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.endereco.controller.EnderecoController;
import com.VanControl.VanControl.endereco.domain.dto.CepResponseDto;
import com.VanControl.VanControl.endereco.exceptions.EnderecoException;
import com.VanControl.VanControl.endereco.exceptions.EnderecoExceptionHandler;
import com.VanControl.VanControl.endereco.service.ViaCepClient;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.veiculo.controller.VeiculoController;
import com.VanControl.VanControl.veiculo.domain.dto.response.VeiculoDefaultResponseDto;
import com.VanControl.VanControl.veiculo.service.VeiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        VeiculoController.class,
        EnderecoController.class
})
@Import({
        SecurityConfig.class,
        SecurityFilter.class,
        CustomAccessDeniedHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomUserDetailsService.class,
        GlobalExceptionHandler.class,
        EnderecoExceptionHandler.class
})
class VeiculoEnderecoWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private VeiculoService service;

    @MockitoBean
    private ViaCepClient viaCep;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository users;

    private static final String SEM_ENDERECO = """
            {"placa":"VAN-1A23","marca":"Mercedes Benz","modelo":"Sprinter",
             "ano":2024,"capacidade":15,"renavam":"12345678901","status":"DISPONIVEL"}
            """;

    private String body(String endereco) {
        return SEM_ENDERECO.trim()
                .replace("}", ",\"endereco\":" + endereco + "}");
    }

    @Test
    void cadastroExigeEndereco() throws Exception {
        mvc.perform(
                        post("/veiculos")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(SEM_ENDERECO)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void cadastroExigeNumero() throws Exception {
        mvc.perform(
                        post("/veiculos")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("{\"cep\":\"01001000\"}"))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void motoristaPodeCadastrarComEndereco() throws Exception {
        when(service.cadastrarVeiculo(any()))
                .thenReturn(
                        new VeiculoDefaultResponseDto(
                                "Veículo cadastrado com sucesso"
                        )
                );

        mvc.perform(
                        post("/veiculos")
                                .with(user("motorista").roles("MOTORISTA"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        body("{\"cep\":\"01001000\",\"numero\":\"100\"}")
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.message")
                                .value("Veículo cadastrado com sucesso")
                );
    }

    @Test
    void passageiroNaoCadastraVeiculo() throws Exception {
        mvc.perform(
                        post("/veiculos")
                                .with(user("passageiro").roles("PASSAGEIRO"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        body("{\"cep\":\"01001000\",\"numero\":\"100\"}")
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void consultaCepPublicaPermitePreencherCadastro() throws Exception {
        when(viaCep.consultar("01001000"))
                .thenReturn(
                        new CepResponseDto(
                                "01001000",
                                "Praça da Sé",
                                "Sé",
                                "São Paulo",
                                "SP",
                                null
                        )
                );

        mvc.perform(get("/enderecos/cep/01001000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01001000"));
    }

    @Test
    void falhaViaCepPossuiStatusECorpoControlados() throws Exception {
        when(viaCep.consultar("01001000"))
                .thenThrow(
                        new EnderecoException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Consulta indisponível"
                        )
                );

        mvc.perform(
                        get("/enderecos/cep/01001000")
                                .with(user("admin").roles("ADMIN"))
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(
                        jsonPath("$.mensagem").value("Consulta indisponível")
                );
    }
}