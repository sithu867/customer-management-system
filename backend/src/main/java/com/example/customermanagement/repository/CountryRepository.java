package com.example.customermanagement.repository;

import com.example.customermanagement.entity.Country;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {

    List<Country> findAllByOrderByCountryNameAsc();
}
