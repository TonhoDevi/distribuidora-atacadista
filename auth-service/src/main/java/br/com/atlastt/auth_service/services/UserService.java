package br.com.atlastt.auth_service.services;

import br.com.atlastt.auth_service.dtos.UserRequestDTO;
import br.com.atlastt.auth_service.dtos.UserResponseDTO;
import br.com.atlastt.auth_service.exceptions.UserNotFoundException;
import br.com.atlastt.auth_service.models.User;
import br.com.atlastt.auth_service.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        String hashedPassword = passwordEncoder.encode(dto.password());
        User user = new User(dto.username(), hashedPassword, dto.role());
        User savedUser = userRepository.save(user);
        return ToResponseDto(savedUser);
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        String hashedPassword = passwordEncoder.encode(dto.password());
        user.setUsername(dto.username());
        user.setPassword(hashedPassword);
        user.setRole(dto.role());
        
        User updatedUser = userRepository.save(user);
        return ToResponseDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::ToResponseDto)
                .collect(Collectors.toList());
    }

    private UserResponseDTO ToResponseDto(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
