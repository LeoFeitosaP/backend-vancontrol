package com.VanControl.VanControl.endereco;

import com.VanControl.VanControl.endereco.domain.dto.CepResponseDto;
import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.VanControl.VanControl.endereco.exceptions.EnderecoException;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.endereco.service.ViaCepClient;
import com.VanControl.VanControl.grafo.domain.entity.PassageiroVeiculoDistancia;
import com.VanControl.VanControl.grafo.repository.PassageiroVeiculoDistanciaRepository;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.veiculo.domain.dto.request.CadastrarVeiculoRequestDto;
import com.VanControl.VanControl.veiculo.repository.VeiculoRepository;
import com.VanControl.VanControl.veiculo.service.VeiculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        VeiculoService.class,
        EnderecoService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VeiculoEnderecoJpaTest {

    @Autowired
    private VeiculoService service;

    @Autowired
    private VeiculoRepository veiculos;

    @Autowired
    private PassageiroRepository passageiros;

    @Autowired
    private PassageiroVeiculoDistanciaRepository distancias;

    @MockitoBean
    private ViaCepClient viaCep;

    @BeforeEach
    void preparar() {
        distancias.deleteAll();
        veiculos.deleteAll();
        passageiros.deleteAll();

        when(viaCep.consultar(anyString()))
                .thenReturn(
                        new CepResponseDto(
                                "01001000",
                                "Praça da Sé",
                                "Sé",
                                "São Paulo",
                                "SP",
                                "lado ímpar"
                        )
                );
    }

    @Test
    void cadastroPersisteTodosOsCamposEConsultaDevolveEndereco() {
        service.cadastrarVeiculo(cadastro());

        var v = veiculos.findByPlaca("VAN-1A23");

        assertNotNull(v);

        var e = v.getEnderecoBase();

        assertEquals("01001000", e.getCep());
        assertEquals("Praça da Sé", e.getLogradouro());
        assertEquals("100", e.getNumero());
        assertEquals("Garagem A", e.getComplemento());
        assertEquals("Sé", e.getBairro());
        assertEquals("São Paulo", e.getCidade());
        assertEquals("SP", e.getUf());

        assertEquals(
                e.formatar(),
                v.getEnderecoInicial()
        );

        var resposta = service.buscarVeiculoPorPlaca("VAN-1A23");

        assertFalse(resposta.enderecoPendente());
        assertEquals("100", resposta.endereco().numero());
    }

    @Test
    void indisponibilidadeNaoSalvaVeiculo() {
        when(viaCep.consultar(anyString()))
                .thenThrow(
                        new EnderecoException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "indisponível"
                        )
                );

        assertThrows(
                EnderecoException.class,
                () -> service.cadastrarVeiculo(cadastro())
        );

        assertEquals(0, veiculos.count());
    }

    @Test
    void alteracaoInvalidaCoordenadasEDistanciasPersistidas() {
        service.cadastrarVeiculo(cadastro());

        var v = veiculos.findByPlaca("VAN-1A23");
        v.setLatitude(-23.5);
        v.setLongitude(-46.6);
        veiculos.saveAndFlush(v);

        var p = passageiros.saveAndFlush(new Passageiro());

        distancias.saveAndFlush(
                PassageiroVeiculoDistancia.builder()
                        .passageiro(p)
                        .veiculo(v)
                        .distanciaKm(10.0)
                        .build()
        );

        service.atualizarEndereco(
                "VAN-1A23",
                endereco("200")
        );

        var atualizado = veiculos.findByPlaca("VAN-1A23");

        assertEquals(
                "200",
                atualizado.getEnderecoBase().getNumero()
        );

        assertNull(atualizado.getLatitude());
        assertNull(atualizado.getLongitude());
        assertEquals(0, distancias.count());
    }

    @Test
    void falhaNaAtualizacaoPreservaEnderecoAnterior() {
        service.cadastrarVeiculo(cadastro());

        when(viaCep.consultar(anyString()))
                .thenThrow(
                        new EnderecoException(
                                HttpStatus.NOT_FOUND,
                                "CEP não encontrado"
                        )
                );

        assertThrows(
                EnderecoException.class,
                () -> service.atualizarEndereco(
                        "VAN-1A23",
                        endereco("200")
                )
        );

        assertEquals(
                "100",
                veiculos.findByPlaca("VAN-1A23")
                        .getEnderecoBase()
                        .getNumero()
        );
    }

    private EnderecoRequestDto endereco(String numero) {
        return new EnderecoRequestDto(
                "01001-000",
                null,
                numero,
                "Garagem A",
                null,
                null,
                null
        );
    }

    private CadastrarVeiculoRequestDto cadastro() {
        return new CadastrarVeiculoRequestDto(
                "VAN-1A23",
                "Mercedes Benz",
                "Sprinter",
                2024,
                15,
                "12345678901",
                "DISPONIVEL",
                endereco("100")
        );
    }
}