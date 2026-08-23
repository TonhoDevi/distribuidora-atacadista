package br.com.atlastt.customer_service.integrations;

import br.com.atlastt.customer_service.models.Customer;
import br.com.atlastt.customer_service.repositories.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class CustomerRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void deveriaSalvarClienteNoBancoReal() {
        Customer customer = new Customer("Maria", "maria@email.com", "99988877766");

        Customer saved = customerRepository.save(customer);

        assertNotNull(saved.getId());
        assertEquals("Maria", saved.getName());
    }
}
