package com.VanControl.VanControl.common.util;

import com.VanControl.VanControl.user.domain.enums.Role;
import com.VanControl.VanControl.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils - Testes de validação de acesso por CPF")
class SecurityUtilsTest {

    @InjectMocks
    private SecurityUtils securityUtils;

    private User passageiroUser;
    private User motoristaUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        passageiroUser = new User();
        passageiroUser.setId(UUID.randomUUID());
        passageiroUser.setName("João Silva");
        passageiroUser.setEmail("joao@email.com");
        passageiroUser.setCpf("12345678901");
        passageiroUser.setRole(Role.PASSAGEIRO);

        motoristaUser = new User();
        motoristaUser.setId(UUID.randomUUID());
        motoristaUser.setName("Maria Santos");
        motoristaUser.setEmail("maria@email.com");
        motoristaUser.setCpf("98765432100");
        motoristaUser.setRole(Role.MOTORISTA);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@email.com");
        adminUser.setCpf("11111111111");
        adminUser.setRole(Role.ADMIN);
    }

    @Test
    @DisplayName("Deve obter o usuário autenticado com sucesso")
    void deveObterUsuarioAutenticado() {
        authenticateUser(passageiroUser);

        User result = securityUtils.getAuthenticatedUser();

        assertNotNull(result);
        assertEquals(passageiroUser.getId(), result.getId());
        assertEquals(passageiroUser.getEmail(), result.getEmail());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não está autenticado")
    void deveLancarExcecaoQuandoUsuarioNaoAutenticado() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class, () -> securityUtils.getAuthenticatedUser());
    }

    @Test
    @DisplayName("Deve obter CPF do usuário autenticado com sucesso")
    void deveObterCpfDoUsuarioAutenticado() {
        authenticateUser(passageiroUser);


        String cpf = securityUtils.getAuthenticatedUserCpf();

        assertEquals("12345678901", cpf);
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não possui CPF")
    void deveLancarExcecaoQuandoUsuarioNaoPossuiCpf() {
        User userSemCpf = new User();
        userSemCpf.setId(UUID.randomUUID());
        userSemCpf.setRole(Role.PASSAGEIRO);
        authenticateUser(userSemCpf);

        assertThrows(AccessDeniedException.class, () -> securityUtils.getAuthenticatedUserCpf());
    }

    @Test
    @DisplayName("Deve permitir que PASSAGEIRO acesse seus próprios dados")
    void devePermitirPassageiroAcessarProprioDados() {
        authenticateUser(passageiroUser);

        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("12345678901"));
    }

    @Test
    @DisplayName("Deve bloquear PASSAGEIRO de acessar dados de outro usuário")
    void deveBloquearPassageiroAcessarDadosDeOutroUsuario() {
        authenticateUser(passageiroUser);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> securityUtils.validateCpfAccess("98765432100"));

        assertEquals("Você não tem permissão para acessar dados de outro usuário", exception.getMessage());
    }

    @Test
    @DisplayName("Deve permitir que ADMIN acesse dados de qualquer CPF")
    void devePermitirAdminAcessarQualquerCpf() {
        authenticateUser(adminUser);

        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("12345678901"));
        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("98765432100"));
        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("00000000000"));
    }

    @Test
    @DisplayName("Deve permitir que MOTORISTA acesse dados de qualquer CPF")
    void devePermitirMotoristaAcessarQualquerCpf() {
        authenticateUser(motoristaUser);

        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("12345678901"));
        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("98765432100"));
        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("00000000000"));
    }

    @Test
    @DisplayName("Deve permitir que MOTORISTA acesse seus próprios dados")
    void devePermitirMotoristaAcessarProprioDados() {
        authenticateUser(motoristaUser);

        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("98765432100"));
    }

    @Test
    @DisplayName("Deve validar que PASSAGEIRO com CPF diferente é bloqueado")
    void deveValidarBloqueioPassageiroComCpfDiferente() {
        authenticateUser(passageiroUser);
        String cpfDiferente = "99999999999";

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> securityUtils.validateCpfAccess(cpfDiferente));

        assertTrue(exception.getMessage().contains("não tem permissão"));
    }

    @Test
    @DisplayName("Deve validar comparação exata de CPF para PASSAGEIRO")
    void deveValidarComparacaoExataDeCpf() {
        authenticateUser(passageiroUser);

        assertDoesNotThrow(() -> securityUtils.validateCpfAccess("12345678901"));

        assertThrows(AccessDeniedException.class,
                () -> securityUtils.validateCpfAccess("12345678900"));
    }

    private void authenticateUser(User user) {
        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
