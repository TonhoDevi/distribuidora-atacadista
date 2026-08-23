package br.com.atlastt.auth_service.services;

import br.com.atlastt.auth_service.dtos.LoginRequestDTO;
import br.com.atlastt.auth_service.dtos.LoginResponseDTO;
import br.com.atlastt.auth_service.exceptions.InvalidCredentialsException;
import br.com.atlastt.auth_service.models.User;
import br.com.atlastt.auth_service.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByUsername(dto.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());

        return new LoginResponseDTO(token, user.getUsername(), user.getRole());
    }
}
