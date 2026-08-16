package br.com.atlastt.customer_service.services;


import br.com.atlastt.customer_service.dtos.CustomerRequestDto;
import br.com.atlastt.customer_service.dtos.CustomerResponseDto;
import br.com.atlastt.customer_service.exceptions.CustomerAlreadyExistsException;
import br.com.atlastt.customer_service.exceptions.CustomerNotFoundException;
import br.com.atlastt.customer_service.models.Customer;
import br.com.atlastt.customer_service.repositories.CustomerRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponseDto createCustomer(CustomerRequestDto customerRequestDto) {
        if (customerRepository.existsByDocument(customerRequestDto.document())) {
            throw new CustomerAlreadyExistsException("Customer already exists with document: " + customerRequestDto.document());
        }
        if (customerRepository.existsByEmail(customerRequestDto.email())) {
            throw new CustomerAlreadyExistsException("Customer already exists with email: " + customerRequestDto.email());
        }
        var customer = customerRepository.save(new Customer(customerRequestDto.name(), customerRequestDto.email(), customerRequestDto.document()));
        return toResponseDto(customer);
    }

    public void deleteCustomer(Long id)  {
        try{
        customerRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomerNotFoundException("Error while deleting customer with id: " + id);
        }
    }

    public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto customerRequestDto) {
        var customer = customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
        customer.setName(customerRequestDto.name());
        customer.setEmail(customerRequestDto.email());
        customer.setDocument(customerRequestDto.document());
        customerRepository.save(customer);
        return toResponseDto(customer);
    }

    public List<CustomerResponseDto> findAllCustomers() {
        return customerRepository.findAll().stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    public CustomerResponseDto findCustomerById(Long id) {
        var customer = customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
        return toResponseDto(customer);
    }


    public List<CustomerResponseDto> findCustomersByName(String name) {
        return customerRepository.findCustomersByName(name).stream().map(this::toResponseDto).collect(Collectors.toList());
    }


    public CustomerResponseDto findCustomerByEmail(String email) {
        var customer = customerRepository.findByEmail(email).orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));
        return toResponseDto(customer);
    }
    public CustomerResponseDto findCustomerByDocument(String document) {
        var customer = customerRepository.findByDocument(document).orElseThrow(() -> new CustomerNotFoundException("Customer not found with document: " + document));
        return toResponseDto(customer);
    }


    private CustomerResponseDto toResponseDto(Customer customer) {
        return new CustomerResponseDto(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getDocument(),
                customer.getCreatedAt()
        );
    }
}

