package com.example.customermanagement.service;

import com.example.customermanagement.dto.CityResponse;
import com.example.customermanagement.dto.CountryResponse;
import com.example.customermanagement.dto.CustomerAddressRequest;
import com.example.customermanagement.dto.CustomerAddressResponse;
import com.example.customermanagement.dto.CustomerRequest;
import com.example.customermanagement.dto.CustomerResponse;
import com.example.customermanagement.dto.CustomerSummaryResponse;
import com.example.customermanagement.entity.City;
import com.example.customermanagement.entity.Country;
import com.example.customermanagement.entity.Customer;
import com.example.customermanagement.entity.CustomerAddress;
import com.example.customermanagement.entity.CustomerFamilyMember;
import com.example.customermanagement.entity.CustomerMobile;
import com.example.customermanagement.exception.ResourceNotFoundException;
import com.example.customermanagement.repository.CityRepository;
import com.example.customermanagement.repository.CountryRepository;
import com.example.customermanagement.repository.CustomerRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    // Main business service for customer CRUD and lookup data.
    private final CustomerRepository customerRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CountryRepository countryRepository,
                           CityRepository cityRepository) {
        this.customerRepository = customerRepository;
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAllDetailed()
                .stream()
                .map(this::mapCustomer)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for id " + id));
        return mapCustomer(customer);
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByNic(request.getNic())) {
            throw new IllegalArgumentException("NIC already exists: " + request.getNic());
        }

        Customer customer = new Customer();
        updateCustomerFields(customer, request);
        Customer savedCustomer = customerRepository.save(customer);
        return getCustomerById(savedCustomer.getId());
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for id " + id));

        if (customerRepository.existsByNicAndIdNot(request.getNic(), id)) {
            throw new IllegalArgumentException("NIC already exists: " + request.getNic());
        }

        updateCustomerFields(customer, request);
        Customer savedCustomer = customerRepository.save(customer);
        return getCustomerById(savedCustomer.getId());
    }

    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found for id " + id);
        }
        customerRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> getCountries() {
        return countryRepository.findAllByOrderByCountryNameAsc()
                .stream()
                .map(country -> CountryResponse.builder()
                        .id(country.getId())
                        .countryName(country.getCountryName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getCities(Long countryId) {
        List<City> cities = countryId == null
                ? cityRepository.findAllByOrderByCityNameAsc()
                : cityRepository.findByCountryIdOrderByCityNameAsc(countryId);

        return cities.stream()
                .map(city -> CityResponse.builder()
                        .id(city.getId())
                        .cityName(city.getCityName())
                        .countryId(city.getCountry().getId())
                        .countryName(city.getCountry().getCountryName())
                        .build())
                .collect(Collectors.toList());
    }

    private void updateCustomerFields(Customer customer, CustomerRequest request) {
        // Accept missing lists as empty so partial requests do not crash the service.
        List<CustomerAddressRequest> addressRequests = listOrEmpty(request.getAddresses());
        List<String> mobileNumbers = listOrEmpty(request.getMobileNumbers());
        List<Long> familyMemberIds = listOrEmpty(request.getFamilyMemberIds());

        customer.setName(request.getName());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setNic(request.getNic());

        // Load referenced records once, then reuse them while rebuilding child collections.
        Map<Long, City> citiesById = mapCitiesById(cityRepository.findAllById(extractIds(addressRequests
                .stream()
                .map(CustomerAddressRequest::getCityId)
                .collect(Collectors.toList()))));
        Map<Long, Country> countriesById = mapCountriesById(countryRepository.findAllById(extractIds(addressRequests
                .stream()
                .map(CustomerAddressRequest::getCountryId)
                .collect(Collectors.toList()))));
        Map<Long, Customer> familyMembersById = mapCustomersById(customerRepository.findAllById(extractIds(familyMemberIds)));

        customer.getMobiles().clear();
        // Rebuild child rows so create and update follow the same path.
        for (String mobileNumber : mobileNumbers) {
            CustomerMobile mobile = new CustomerMobile();
            mobile.setCustomer(customer);
            mobile.setMobileNumber(mobileNumber);
            customer.getMobiles().add(mobile);
        }

        customer.getAddresses().clear();
        for (CustomerAddressRequest addressRequest : addressRequests) {
            CustomerAddress address = new CustomerAddress();
            address.setCustomer(customer);
            address.setAddressLine1(addressRequest.getAddressLine1());
            address.setAddressLine2(addressRequest.getAddressLine2());
            address.setCity(resolveEntity("City", addressRequest.getCityId(), citiesById));
            address.setCountry(resolveEntity("Country", addressRequest.getCountryId(), countriesById));
            customer.getAddresses().add(address);
        }

        customer.getFamilyMembers().clear();
        for (Long familyMemberId : new LinkedHashSet<>(familyMemberIds)) {
            if (Objects.equals(customer.getId(), familyMemberId)) {
                continue;
            }

            CustomerFamilyMember familyMember = new CustomerFamilyMember();
            familyMember.setCustomer(customer);
            familyMember.setFamilyMemberCustomer(resolveEntity("Customer", familyMemberId, familyMembersById));
            customer.getFamilyMembers().add(familyMember);
        }
    }

    private static <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private <T> T resolveEntity(String label, Long id, Map<Long, T> entitiesById) {
        if (id == null) {
            return null;
        }
        T entity = entitiesById.get(id);
        if (entity == null) {
            throw new ResourceNotFoundException(label + " not found for id " + id);
        }
        return entity;
    }

    private Map<Long, City> mapCitiesById(Iterable<City> cities) {
        Map<Long, City> citiesById = new LinkedHashMap<>();
        for (City city : cities) {
            citiesById.put(city.getId(), city);
        }
        return citiesById;
    }

    private Map<Long, Country> mapCountriesById(Iterable<Country> countries) {
        Map<Long, Country> countriesById = new LinkedHashMap<>();
        for (Country country : countries) {
            countriesById.put(country.getId(), country);
        }
        return countriesById;
    }

    private Map<Long, Customer> mapCustomersById(Iterable<Customer> customers) {
        Map<Long, Customer> customersById = new LinkedHashMap<>();
        for (Customer familyMember : customers) {
            customersById.put(familyMember.getId(), familyMember);
        }
        return customersById;
    }

    private List<Long> extractIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private CustomerResponse mapCustomer(Customer customer) {
        // Convert database entities into safe DTOs for the API response.
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .dateOfBirth(customer.getDateOfBirth())
                .nic(customer.getNic())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .mobileNumbers(customer.getMobiles()
                        .stream()
                        .map(CustomerMobile::getMobileNumber)
                        .collect(Collectors.toList()))
                .addresses(customer.getAddresses()
                        .stream()
                        .map(address -> CustomerAddressResponse.builder()
                                .id(address.getId())
                                .addressLine1(address.getAddressLine1())
                                .addressLine2(address.getAddressLine2())
                                .cityId(address.getCity() != null ? address.getCity().getId() : null)
                                .cityName(address.getCity() != null ? address.getCity().getCityName() : null)
                                .countryId(address.getCountry() != null ? address.getCountry().getId() : null)
                                .countryName(address.getCountry() != null ? address.getCountry().getCountryName() : null)
                                .build())
                        .collect(Collectors.toList()))
                .familyMembers(customer.getFamilyMembers()
                        .stream()
                        .map(familyMember -> CustomerSummaryResponse.builder()
                                .id(familyMember.getFamilyMemberCustomer().getId())
                                .name(familyMember.getFamilyMemberCustomer().getName())
                                .nic(familyMember.getFamilyMemberCustomer().getNic())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
