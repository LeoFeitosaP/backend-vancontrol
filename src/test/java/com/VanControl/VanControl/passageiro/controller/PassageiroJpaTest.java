package com.VanControl.VanControl.passageiro.controller;

import com.VanControl.VanControl.common.exception.model.ConflictException;
import com.VanControl.VanControl.common.security.TokenService;
import com.VanControl.VanControl.common.service.CredentialsService;
import com.VanControl.VanControl.endereco.domain.dto.CepResponseDto;
import com.VanControl.VanControl.endereco.domain.dto.EnderecoRequestDto;
import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.endereco.exceptions.EnderecoException;
import com.VanControl.VanControl.endereco.service.EnderecoService;
import com.VanControl.VanControl.endereco.service.ViaCepClient;
import com.VanControl.VanControl.grafo.domain.entity.PassageiroVeiculoDistancia;
import com.VanControl.VanControl.grafo.repository.PassageiroVeiculoDistanciaRepository;
import com.VanControl.VanControl.motorista.domain.dto.request.CadastrarMotoristaRequestDto;
import com.VanControl.VanControl.motorista.repository.MotoristaRepository;
import com.VanControl.VanControl.motorista.service.MotoristaService;
import com.VanControl.VanControl.passageiro.domain.dto.request.AtualizarPassageiroRequestDto;
import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.passageiro.repository.PassageiroRepository;
import com.VanControl.VanControl.passageiro.service.PassageiroService;
import com.VanControl.VanControl.user.Repository.UserRepository;
import com.VanControl.VanControl.user.domain.dto.request.RegisterRequestDTO;
import com.VanControl.VanControl.user.domain.entity.User;
import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.user.service.EmailService;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.veiculo.repository.VeiculoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        CredentialsService.class,
        PassageiroService.class,
        EnderecoService.class,
        MotoristaService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PassageiroEnderecoJpaTest {

    @Autowired
    private CredentialsService credentials;

    @Autowired
    private PassageiroService service;

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private PassageiroRepository passageiros;

    @Autowired
    private UserRepository users;

    @Autowired
    private MotoristaRepository motoristas;

    @Autowired
    private VeiculoRepository veiculos;

    @Autowired
    private PassageiroVeiculoDistanciaRepository distancias;

    @MockitoBean
    private ViaCepClient viaCep;

    @MockitoBean
    private PasswordEncoder encoder;

    @MockitoBean
    private TokenService tokens;

    @MockitoBean
    private EmailService emails;

    @BeforeEach
    void preparar() {
        distancias.deleteAll();
        passageiros.deleteAll();
        motoristas.deleteAll();
        veiculos.deleteAll();
        users.deleteAll();

        when(encoder.encode(any())).thenReturn("hash");
        when(tokens.generateToken(any())).thenReturn("token");

        when(viaCep.consultar(anyString())).thenAnswer(invocation -> {
            assertFalse(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );

            return new CepResponseDto(
                    "01001000",
                    "Praça da Sé",
                    "Sé",
                    "São Paulo",
                    "SP",
                    null
            );
        });

        var admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(Role.ADMIN);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cadastroPersisteEstruturaETextoEUsuarioNaMesmaOperacao() {
        credentials.registrarUsuario(
                cadastro(
                        "02245419006",
                        "ana@example.com",
                        "(11) 99999-1111"
                )
        );

        var p = passageiro();
        var e = p.getEnderecoDetalhado();

        assertEquals(
                new Endereco(
                        "01001000",
                        "Praça da Sé",
                        "100",
                        "Apto 12",
                        "Sé",
                        "São Paulo",
                        "SP"
                ),
                e
        );

        assertEquals(e.formatar(), p.getEndereco());
        assertEquals(e.getCep(), p.getCep());
        assertEquals(Role.PASSAGEIRO, p.getUser().getRole());
        assertEquals(1, users.count());

        assertFalse(
                service.buscarPassageiroPorCpf("02245419006")
                        .enderecoPendente()
        );
    }

    @Test
    void falhaViaCepNaoCriaUsuarioNemPassageiro() {
        when(viaCep.consultar(anyString())).thenThrow(
                new EnderecoException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "indisponível"
                )
        );

        assertThrows(
                EnderecoException.class,
                () -> credentials.registrarUsuario(
                        cadastro(
                                "02245419006",
                                "ana@example.com",
                                "(11) 99999-1111"
                        )
                )
        );

        assertEquals(0, users.count());
        assertEquals(0, passageiros.count());
    }

    @Test
    void erroAoPersistirPassageiroDesfazNovoUsuario() {
        credentials.registrarUsuario(
                cadastro(
                        "02245419006",
                        "ana@example.com",
                        "(11) 99999-1111"
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> credentials.registrarUsuario(
                        cadastro(
                                "52998224725",
                                "bia@example.com",
                                "(11) 99999-1111"
                        )
                )
        );

        assertEquals(1, users.count());
        assertEquals(1, passageiros.count());
        assertTrue(users.findByEmail("bia@example.com").isEmpty());
    }

    @Test
    void mudancaInvalidaCoordenadasEDistancias() {
        prepararDistancia();

        service.atualizarPassageiro(
                "02245419006",
                atualizacao(endereco("200"))
        );

        var p = passageiro();

        assertEquals("200", p.getEnderecoDetalhado().getNumero());
        assertNull(p.getLatitude());
        assertNull(p.getLongitude());
        assertEquals(0, distancias.count());
    }

    @Test
    void mesmoEnderecoPreservaCoordenadasEDistancias() {
        prepararDistancia();

        service.atualizarPassageiro(
                "02245419006",
                atualizacao(endereco("100"))
        );

        assertEquals(-23.5, passageiro().getLatitude());
        assertEquals(1, distancias.count());
    }

    @Test
    void atualizacaoSemEnderecoNaoConsultaViaCep() {
        prepararDistancia();
        clearInvocations(viaCep);

        service.atualizarPassageiro(
                "02245419006",
                atualizacao(null)
        );

        verifyNoInteractions(viaCep);

        assertEquals(1, distancias.count());
        assertEquals(
                "100",
                passageiro().getEnderecoDetalhado().getNumero()
        );
    }

    @Test
    void falhaViaCepPreservaEnderecoCoordenadasEDistancias() {
        prepararDistancia();

        when(viaCep.consultar(anyString())).thenThrow(
                new EnderecoException(
                        HttpStatus.NOT_FOUND,
                        "CEP não encontrado"
                )
        );

        assertThrows(
                EnderecoException.class,
                () -> service.atualizarPassageiro(
                        "02245419006",
                        atualizacao(endereco("200"))
                )
        );

        assertEquals(
                "100",
                passageiro().getEnderecoDetalhado().getNumero()
        );

        assertEquals(-23.5, passageiro().getLatitude());
        assertEquals(1, distancias.count());
    }

    @Test
    void conflitoDeEmailPreservaEnderecoECache() {
        prepararDistancia();

        var outro = new User();
        outro.setEmail("outro@example.com");
        users.saveAndFlush(outro);

        var dto = new AtualizarPassageiroRequestDto(
                null,
                null,
                "outro@example.com",
                null,
                null,
                endereco("200")
        );

        assertThrows(
                ConflictException.class,
                () -> service.atualizarPassageiro("02245419006", dto)
        );

        assertEquals(
                "100",
                passageiro().getEnderecoDetalhado().getNumero()
        );

        assertEquals(1, distancias.count());
    }

    @Test
    void motoristaContinuaSemEnderecoOuPassageiro() {
        motoristaService.cadastrarMotorista(
                new CadastrarMotoristaRequestDto(
                        "Carlos Silva",
                        "carlos@example.com",
                        "98765432101",
                        "D",
                        java.time.YearMonth.of(2099, 12),
                        "52998224725",
                        "(11) 98888-1111",
                        "Senha123"
                )
        );

        assertEquals(1, motoristas.count());
        assertEquals(0, passageiros.count());

        assertEquals(
                Role.MOTORISTA,
                users.findByEmail("carlos@example.com")
                        .orElseThrow()
                        .getRole()
        );

        verifyNoInteractions(viaCep);
    }

    @Test
    void legadoPermaneceConsultavelEAceitaRegularizacao() {
        var user = new User();
        user.setCpf("02245419006");
        user.setEmail("legado@example.com");
        user.setRole(Role.PASSAGEIRO);

        var p = new Passageiro(
                "(11) 99999-1111",
                "UNISO",
                "Noite",
                "Texto antigo sem estrutura",
                "01001-000"
        );

        p.setUser(user);
        passageiros.saveAndFlush(p);

        var antes = service.buscarPassageiroPorCpf("02245419006");

        assertTrue(antes.enderecoPendente());
        assertEquals("Texto antigo sem estrutura", antes.endereco());
        assertNull(antes.enderecoDetalhado());

        var depois = service.atualizarPassageiro(
                "02245419006",
                atualizacao(endereco("100"))
        );

        assertFalse(depois.enderecoPendente());
        assertEquals("01001000", depois.enderecoDetalhado().cep());
    }

    private void prepararDistancia() {
        credentials.registrarUsuario(
                cadastro(
                        "02245419006",
                        "ana@example.com",
                        "(11) 99999-1111"
                )
        );

        var p = passageiro();
        p.setLatitude(-23.5);
        p.setLongitude(-46.6);
        passageiros.saveAndFlush(p);

        var v = veiculos.saveAndFlush(
                new Veiculo(
                        "TST-1A23",
                        "Mercedes Benz",
                        "Sprinter",
                        2024,
                        15,
                        "12345678901",
                        "DISPONIVEL"
                )
        );

        distancias.saveAndFlush(
                PassageiroVeiculoDistancia.builder()
                        .passageiro(p)
                        .veiculo(v)
                        .distanciaKm(10.0)
                        .build()
        );
    }

    private Passageiro passageiro() {
        return passageiros.findByUser_Cpf("02245419006")
                .orElseThrow();
    }

    private EnderecoRequestDto endereco(String numero) {
        return new EnderecoRequestDto(
                "01001-000",
                null,
                numero,
                "Apto 12",
                null,
                null,
                null
        );
    }

    private RegisterRequestDTO cadastro(
            String cpf,
            String email,
            String telefone
    ) {
        return new RegisterRequestDTO(
                "Ana Silva",
                email,
                "Senha123",
                cpf,
                telefone,
                "UNISO",
                "Noite",
                endereco("100")
        );
    }

    private AtualizarPassageiroRequestDto atualizacao(
            EnderecoRequestDto endereco
    ) {
        return new AtualizarPassageiroRequestDto(
                null,
                null,
                null,
                null,
                null,
                endereco
        );
    }
}