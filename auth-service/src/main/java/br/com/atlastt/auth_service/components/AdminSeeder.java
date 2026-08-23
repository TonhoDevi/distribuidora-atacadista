package br.com.atlastt.auth_service.components;

import br.com.atlastt.auth_service.models.User;
import br.com.atlastt.auth_service.models.UserRole;
import br.com.atlastt.auth_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.default-password}") String defaultPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User("TonhoDevi", passwordEncoder.encode(defaultPassword), UserRole.ADMIN);
            userRepository.save(admin);
            System.out.println("Admin padrão criado com senha temporária. Troque assim que possível.");
        }
    }
}
