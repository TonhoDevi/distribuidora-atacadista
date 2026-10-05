package br.com.atlastt.auth_service.services;

import br.com.atlastt.auth_service.dtos.LoginRequestDTO;
import br.com.atlastt.auth_service.exceptions.InvalidCredentialsException;
import br.com.atlastt.auth_service.models.User;
import br.com.atlastt.auth_service.models.UserRole;
import br.com.atlastt.auth_service.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void deveriaFazerLoginEGerarTokenQuandoCredenciaisCorretas() {
        // Arrange
        var user = new User("TonhoDevi", "hash", UserRole.ADMIN);
        when(userRepository.findByUsername("TonhoDevi")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha", "hash")).thenReturn(true);
        when(jwtService.generateToken("TonhoDevi", "ADMIN")).thenReturn("jwt-token");

        // Act
        var resultado = authService.login(new LoginRequestDTO("TonhoDevi", "senha"));

        // Assert
        assertEquals("jwt-token", resultado.token());
        assertEquals("TonhoDevi", resultado.username());
        assertEquals(UserRole.ADMIN, resultado.role());
    }

    @Test
    void deveriaLancarExcecaoQuandoSenhaIncorreta() {
        // Arrange
        var user = new User("TonhoDevi", "hash", UserRole.ADMIN);
        when(userRepository.findByUsername("TonhoDevi")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO("TonhoDevi", "errada")));
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void deveriaLancarExcecaoQuandoUsuarioNaoExiste() {
        // Arrange
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO("fantasma", "qualquer")));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void deveriaUsarMesmaMensagemParaUsuarioInexistenteESenhaErrada() {
        // Arrange: protege contra "user enumeration" (não revelar se o usuário existe)
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());
        var user = new User("TonhoDevi", "hash", UserRole.ADMIN);
        when(userRepository.findByUsername("TonhoDevi")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        // Act
        var semUsuario = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO("fantasma", "x")));
        var senhaErrada = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO("TonhoDevi", "errada")));

        // Assert
        assertEquals(semUsuario.getMessage(), senhaErrada.getMessage());
    }
}
