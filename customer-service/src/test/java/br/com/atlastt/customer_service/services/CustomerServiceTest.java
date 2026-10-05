package br.com.atlastt.customer_service.services;

import br.com.atlastt.customer_service.dtos.CustomerRequestDto;
import br.com.atlastt.customer_service.exceptions.CustomerAlreadyExistsException;
import br.com.atlastt.customer_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.customer_service.models.Customer;
import br.com.atlastt.customer_service.repositories.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void deveriaCriarClienteComSucesso() {
        // Arrange
        var dto = new CustomerRequestDto("João", "joao@email.com", "12345678900");
        when(customerRepository.existsByDocument(dto.document())).thenReturn(false);
        when(customerRepository.existsByEmail(dto.email())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(
                new Customer("João", "joao@email.com", "12345678900"));

        // Act
        var resultado = customerService.createCustomer(dto);

        // Assert
        assertEquals("João", resultado.name());
        assertEquals("joao@email.com", resultado.email());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void deveriaLancarExcecaoQuandoDocumentoJaExiste() {
        // Arrange
        var dto = new CustomerRequestDto("João", "joao@email.com", "12345678900");
        when(customerRepository.existsByDocument(dto.document())).thenReturn(true);

        // Act & Assert
        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.createCustomer(dto));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void deveriaLancarExcecaoQuandoEmailJaExiste() {
        // Arrange
        var dto = new CustomerRequestDto("João", "joao@email.com", "12345678900");
        when(customerRepository.existsByDocument(dto.document())).thenReturn(false);
        when(customerRepository.existsByEmail(dto.email())).thenReturn(true);

        // Act & Assert
        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.createCustomer(dto));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void deveriaEncontrarClientePorIdComSucesso() {
        // Arrange
        Customer customer = new Customer("João", "joao@email.com", "12345678900");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act
        var resultado = customerService.findCustomerById(1L);

        // Assert
        assertEquals("João", resultado.name());
    }

    @Test
    void deveriaLancarExcecaoQuandoNaoEncontrarClientePorId() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.findCustomerById(1L));
    }

    @Test
    void deveriaListarTodosOsClientes() {
        // Arrange
        when(customerRepository.findAll()).thenReturn(List.of(
                new Customer("João", "joao@email.com", "111"),
                new Customer("Maria", "maria@email.com", "222")));

        // Act
        var resultado = customerService.findAllCustomers();

        // Assert
        assertEquals(2, resultado.size());
        assertEquals("Maria", resultado.get(1).name());
    }

    @Test
    void deveriaAtualizarClienteComSucesso() {
        // Arrange
        Customer customer = new Customer("João", "joao@email.com", "12345678900");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        var dto = new CustomerRequestDto("João Atualizado", "novo@email.com", "12345678900");

        // Act
        var resultado = customerService.updateCustomer(1L, dto);

        // Assert
        assertEquals("João Atualizado", resultado.name());
        assertEquals("novo@email.com", resultado.email());
        verify(customerRepository).save(customer);
    }

    @Test
    void deveriaLancarExcecaoAoAtualizarClienteInexistente() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        var dto = new CustomerRequestDto("João", "joao@email.com", "12345678900");

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.updateCustomer(1L, dto));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void deveriaDeletarClienteComSucesso() {
        // Arrange
        doNothing().when(customerRepository).deleteById(1L);

        // Act
        customerService.deleteCustomer(1L);

        // Assert
        verify(customerRepository, times(1)).deleteById(1L);
    }

    @Test
    void deveriaLancarExcecaoAoDeletarClienteInexistente() {
        // Arrange
        doThrow(new EmptyResultDataAccessException(1)).when(customerRepository).deleteById(99L);

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.deleteCustomer(99L));
    }
}
