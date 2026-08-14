package br.com.atlastt.customer_service.repositories;


import br.com.atlastt.customer_service.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


public interface CustomerRepository extends JpaRepository<Customer, Long> {

    public Optional<Customer> findByEmail(String email);
    public Optional<Customer> findByDocument(String document);
    public List<Customer> findCustomersByName(String name);
}
