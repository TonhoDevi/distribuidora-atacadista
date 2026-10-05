package br.com.atlastt.auth_service.services;

import br.com.atlastt.auth_service.dtos.UserRequestDTO;
import br.com.atlastt.auth_service.exceptions.UserNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void deveriaCriarUsuarioComSenhaCriptografada() {
        // Arrange
        var dto = new UserRequestDTO("admin", "senha123", UserRole.ADMIN);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var resultado = userService.createUser(dto);

        // Assert
        assertEquals("admin", resultado.username());
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed_password")));
    }

    @Test
    void deveriaDeletarUsuarioComSucesso() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deveriaLancarExcecaoAoDeletarUsuarioInexistente() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deveriaAtualizarUsuarioComSucesso() {
        // Arrange
        User existente = new User("admin", "old_pass", UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode("new_pass")).thenReturn("new_hashed_pass");
        when(userRepository.save(any(User.class))).thenReturn(existente);
        var dto = new UserRequestDTO("admin_updated", "new_pass", UserRole.GERENTE);

        // Act
        var resultado = userService.updateUser(1L, dto);

        // Assert
        assertEquals("admin_updated", resultado.username());
        assertEquals(UserRole.GERENTE, resultado.role());
        assertEquals("new_hashed_pass", existente.getPassword());
    }
}
