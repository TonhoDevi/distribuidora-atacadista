package br.com.atlastt.customer_service.services;


import br.com.atlastt.customer_service.models.Customer;
import br.com.atlastt.customer_service.repositories.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public void createCustomer(String name, String email, String document) {
        if (findCustomerByDocument(document) != null) {
            throw new RuntimeException("Customer already exists with document: " + document);
        }
        if (findCustomerByEmail(email) != null) {
            throw new RuntimeException("Customer already exists with email: " + email);
        }
        var customer = new Customer(name, email, document);
        customerRepository.save(customer);
    }

    public void deleteCustomer(Long id)  {
        try{
        customerRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Error while deleting customer with id: " + id, e);
        }
    }

    public Customer updateCustomer(Long id, String name, String email, String document) {
        var customer = customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customer.setName(name);
        customer.setEmail(email);
        customer.setDocument(document);
        customerRepository.save(customer);
        return customer;
    }

    public Customer findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email).orElse(null);
    }
    public Customer findCustomerByDocument(String document) {
        return customerRepository.findByDocument(document).orElse(null);
    }

    public Customer findCustomerById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    public List<Customer> findAllCustomers() {
        return customerRepository.findAll();
    }
    public List<Customer> findCustomersByName(String name) {
        return customerRepository.findCustomersByName(name);
    }

}
