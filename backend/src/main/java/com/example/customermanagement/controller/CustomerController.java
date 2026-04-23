package com.example.customermanagement.controller;

import com.example.customermanagement.dto.CityResponse;
import com.example.customermanagement.dto.BulkImportJobResponse;
import com.example.customermanagement.dto.BulkImportMode;
import com.example.customermanagement.dto.BulkImportResponse;
import com.example.customermanagement.dto.CountryResponse;
import com.example.customermanagement.dto.CustomerRequest;
import com.example.customermanagement.dto.CustomerResponse;
import com.example.customermanagement.service.BulkCustomerImportService;
import com.example.customermanagement.service.BulkImportJobService;
import com.example.customermanagement.service.CustomerService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class CustomerController {

    private final CustomerService customerService;
    private final BulkCustomerImportService bulkCustomerImportService;
    private final BulkImportJobService bulkImportJobService;

    public CustomerController(CustomerService customerService,
                              BulkCustomerImportService bulkCustomerImportService,
                              BulkImportJobService bulkImportJobService) {
        this.customerService = customerService;
        this.bulkCustomerImportService = bulkCustomerImportService;
        this.bulkImportJobService = bulkImportJobService;
    }

    @GetMapping("/customers")
    public List<CustomerResponse> getCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/customers/{id}")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest request) {
        return customerService.createCustomer(request);
    }

    @PutMapping("/customers/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @DeleteMapping("/customers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
    }

    @GetMapping("/countries")
    public List<CountryResponse> getCountries() {
        return customerService.getCountries();
    }

    @GetMapping("/cities")
    public List<CityResponse> getCities(@RequestParam(required = false) Long countryId) {
        return customerService.getCities(countryId);
    }

    @PostMapping(value = "/customers/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BulkImportResponse bulkImportCustomers(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(defaultValue = "UPSERT") BulkImportMode mode) {
        return bulkCustomerImportService.importCustomers(file, mode);
    }

    @PostMapping(value = "/customers/bulk-jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BulkImportJobResponse queueBulkImportCustomers(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(defaultValue = "UPSERT") BulkImportMode mode) {
        return bulkImportJobService.queueImport(file, mode);
    }

    @GetMapping("/customers/bulk-jobs/{jobId}")
    public BulkImportJobResponse getBulkImportJob(@PathVariable String jobId) {
        return bulkImportJobService.getJob(jobId);
    }
}
