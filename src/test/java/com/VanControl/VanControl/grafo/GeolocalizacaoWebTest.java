package com.VanControl.VanControl.grafo;

import com.VanControl.VanControl.common.exception.GlobalExceptionHandler;
import com.VanControl.VanControl.common.security.CustomAccessDeniedHandler;
import com.VanControl.VanControl.common.security.CustomAuthenticationEntryPoint;
import com.VanControl.VanControl.common.security.CustomUserDetailsService;
import com.VanControl.VanControl.common.security.SecurityConfig;
import com.VanControl.VanControl.common.security.SecurityFilter;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.endereco.exceptions.EnderecoExceptionHandler;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.grafo.controller.GeolocalizacaoController;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Distancia;
import com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.Geocodificacao;
import com.VanControl.VanControl.grafo.service.NominatimClient;
import com.VanControl.VanControl.grafo.service.OsrmClient;
import com.VanControl.VanControl.user.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.EstadoDistancia.ESTIMADA;
import static com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.EstadoGeocodificacao.AMBIGUA;
import static com.VanControl.VanControl.grafo.domain.dto.ConsultaGeografica.MetodoDistancia.HAVERSINE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeolocalizacaoController.class)
@Import({
        SecurityConfig.class,
        SecurityFilter.class,
        CustomAccessDeniedHandler.class,
        CustomAuthenticationEntryPoint.class,
        CustomUserDetailsService.class,
        GlobalExceptionHandler.class,
        EnderecoExceptionHandler.class
})
class GeolocalizacaoWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EnderecoService enderecos;

    @MockitoBean
    private NominatimClient nominatim;

    @MockitoBean
    private OsrmClient osrm;

    @MockitoBean
    private TokenService tokens;

    @MockitoBean
    private UserRepository users;

    private static final String ENDERECO =
            "{\"cep\":\"01001000\",\"numero\":\"100\"}";

    private static final String DISTANCIA = """
            {"origem":{"latitude":-23.5505,"longitude":-46.6333},
             "destino":{"latitude":-23.56,"longitude":-46.64},"permitirEstimativa":true}
            """;

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(
                        post("/geolocalizacao/enderecos/consultar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ENDERECO)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(enderecos, nominatim, osrm);
    }

    @Test
    void passageiroNaoPodeConsultarEnderecoArbitrario() throws Exception {
        mvc.perform(
                        post("/geolocalizacao/enderecos/consultar")
                                .with(user("passageiro").roles("PASSAGEIRO"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ENDERECO)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(enderecos, nominatim, osrm);
    }

    @Test
    void numeroObrigatorioAntesDaIntegracao() throws Exception {
        mvc.perform(
                        post("/geolocalizacao/enderecos/consultar")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cep\":\"01001000\"}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(enderecos, nominatim, osrm);
    }

    @Test
    void ambiguidadeRetorna422SemCoordenadas() throws Exception {
        var endereco = new Endereco(
                "01001000",
                "Praça da Sé",
                "100",
                null,
                "Sé",
                "São Paulo",
                "SP"
        );

        when(enderecos.resolver(any())).thenReturn(endereco);

        when(nominatim.consultar(endereco))
                .thenReturn(new Geocodificacao(
                        AMBIGUA,
                        null,
                        "Revise o endereço",
                        "OSM"
                ));

        mvc.perform(
                        post("/geolocalizacao/enderecos/consultar")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ENDERECO)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.estado").value("AMBIGUA"))
                .andExpect(jsonPath("$.coordenadas").doesNotExist());
    }

    @Test
    void estimativaTemMetodoEMotivoNaResposta() throws Exception {
        when(osrm.consultar(any(), any(), eq(true)))
                .thenReturn(new Distancia(
                        ESTIMADA,
                        HAVERSINE,
                        1.2,
                        "INDISPONIVEL",
                        "Linha reta"
                ));

        mvc.perform(
                        post("/geolocalizacao/distancias/consultar")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(DISTANCIA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metodo").value("HAVERSINE"))
                .andExpect(jsonPath("$.motivo").value("INDISPONIVEL"));
    }

    @Test
    void coordenadaForaDaFaixaRetorna400() throws Exception {
        mvc.perform(
                        post("/geolocalizacao/distancias/consultar")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(DISTANCIA.replace("-23.5505", "100"))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(osrm);
    }
}