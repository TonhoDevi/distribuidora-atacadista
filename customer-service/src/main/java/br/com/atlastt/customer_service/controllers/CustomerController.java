package br.com.atlastt.customer_service.controllers;

import br.com.atlastt.customer_service.dtos.CustomerRequestDto;
import br.com.atlastt.customer_service.dtos.CustomerResponseDto;
import br.com.atlastt.customer_service.models.Customer;
import br.com.atlastt.customer_service.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CustomerRequestDto customer) {
        return new ResponseEntity<>(customerService.createCustomer(customer), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAllCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findCustomerById(id));
    }

    @GetMapping("/by-email")
    public ResponseEntity<CustomerResponseDto> getCustomerByEmail(@RequestParam String email) {
        return ResponseEntity.ok(customerService.findCustomerByEmail(email));
    }

    @GetMapping("/by-document")
    public ResponseEntity<CustomerResponseDto> getCustomerByDocument(@RequestParam String document) {
        return ResponseEntity.ok(customerService.findCustomerByDocument(document));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponseDto>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(customerService.findCustomersByName(name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequestDto customer) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}