package br.com.atlastt.customer_service.controllers;

import br.com.atlastt.customer_service.dtos.CustomerRequestDto;
import br.com.atlastt.customer_service.dtos.CustomerResponseDto;
import br.com.atlastt.customer_service.models.Customer;
import br.com.atlastt.customer_service.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public CustomerResponseDto createCustomer(@Valid @RequestBody CustomerRequestDto customer) {
        return customerService.createCustomer(customer);
    }

    @GetMapping
    public List<CustomerResponseDto> getAllCustomers() {
        return customerService.findAllCustomers();
    }

    @GetMapping("/{id}")
    public CustomerResponseDto getCustomerById(@PathVariable Long id) {
        return customerService.findCustomerById(id);
    }

    @GetMapping("/by-email")
    public CustomerResponseDto getCustomerByEmail(@RequestParam String email) {
        return customerService.findCustomerByEmail(email);
    }

    @GetMapping("/by-document")
    public CustomerResponseDto getCustomerByDocument(@RequestParam String document) {
        return customerService.findCustomerByDocument(document);
    }

    @GetMapping("/search")
    public List<CustomerResponseDto> searchByName(@RequestParam String name) {
        return customerService.findCustomersByName(name);
    }

    @PutMapping("/{id}")
    public CustomerResponseDto updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequestDto customer) {
        return customerService.updateCustomer(id, customer);
    }

    @DeleteMapping("/{id}")
    public void deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
    }
}