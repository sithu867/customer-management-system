package com.example.customermanagement;

import com.example.customermanagement.dto.CustomerRequest;
import com.example.customermanagement.dto.CustomerResponse;
import com.example.customermanagement.repository.CustomerRepository;
import com.example.customermanagement.service.CustomerService;
import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomerServiceIntegrationTests {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void createAndUpdateCustomer() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Jane Doe");
        request.setDateOfBirth(LocalDate.of(1992, 6, 15));
        request.setNic("923456789V");
        request.getMobileNumbers().add("0771234567");

        CustomerResponse created = customerService.createCustomer(request);

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals("Jane Doe", created.getName());
        Assertions.assertEquals(1, customerService.getAllCustomers().size());

        request.setName("Jane Smith");
        CustomerResponse updated = customerService.updateCustomer(created.getId(), request);

        Assertions.assertEquals("Jane Smith", updated.getName());
        Assertions.assertEquals("923456789V", updated.getNic());
    }
}
