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
        var dto = new CustomerRequestDto("João", "joao@email.com", "12345678900");
        when(customerRepository.existsByDocument(dto.document())).thenReturn(false);
        when(customerRepository.existsByEmail(dto.email())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(
                new Customer("João", "joao@email.com", "12345678900")
        );
        var resultado = customerService.createCustomer(dto);

        assertEquals("João", resultado.name());
    }

    @Test
    void deveriaLancarExcecaoQuandoDocumentoJaExiste() {
        var dto = new CustomerRequestDto("João", "joao@email.com", "12345678900");
        when(customerRepository.existsByDocument(dto.document())).thenReturn(true);
        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.createCustomer(dto));
    }

    @Test
    void deveriaLancarExcecaoQuandoEmailJaExiste() {
        var dto = new CustomerRequestDto("João", "joao@email.com", "12345678900");
        when(customerRepository.existsByDocument(dto.document())).thenReturn(false);
        when(customerRepository.existsByEmail(dto.email())).thenReturn(true);

        assertThrows(CustomerAlreadyExistsException.class, () -> customerService.createCustomer(dto));
    }

    @Test
    void deveriaEncontrarClientePorIdComSucesso() {
        Customer customer = new Customer("João", "joao@email.com", "12345678900");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        var resultado = customerService.findCustomerById(1L);

        assertEquals("João", resultado.name());
    }

    @Test
    void deveriaLancarExcecaoQuandoNaoEncontrarClientePorId() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(CustomerNotFoundException.class, () -> customerService.findCustomerById(1L));
    }

    @Test
    void deveriaAtualizarClienteComSucesso() {
        Customer customer = new Customer("João", "joao@email.com", "12345678900");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        
        var dto = new CustomerRequestDto("João Atualizado", "novo@email.com", "12345678900");
        var resultado = customerService.updateCustomer(1L, dto);

        assertEquals("João Atualizado", resultado.name());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void deveriaDeletarClienteComSucesso() {
        doNothing().when(customerRepository).deleteById(1L);
        customerService.deleteCustomer(1L);
        verify(customerRepository, times(1)).deleteById(1L);
    }
}
