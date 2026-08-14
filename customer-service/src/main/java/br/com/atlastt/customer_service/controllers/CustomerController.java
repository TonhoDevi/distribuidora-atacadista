package br.com.atlastt.customer_service.controllers;

import br.com.atlastt.customer_service.models.Customer;
import br.com.atlastt.customer_service.services.CustomerService;
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
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerService.createCustomer(
                customer.getName(), customer.getEmail(), customer.getDocument()
        );
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerService.findAllCustomers();
    }

    @GetMapping("/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerService.findCustomerById(id);
    }

    @GetMapping("/by-email")
    public Customer getCustomerByEmail(@RequestParam String email) {
        return customerService.findCustomerByEmail(email);
    }

    @GetMapping("/by-document")
    public Customer getCustomerByDocument(@RequestParam String document) {
        return customerService.findCustomerByDocument(document);
    }

    @GetMapping("/search")
    public List<Customer> searchByName(@RequestParam String name) {
        return customerService.findCustomersByName(name);
    }

    @PutMapping("/{id}")
    public Customer updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        return customerService.updateCustomer(
                id, customer.getName(), customer.getEmail(), customer.getDocument()
        );
    }

    @DeleteMapping("/{id}")
    public void deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
    }
}