package com.example.customermanagement.repository;

import com.example.customermanagement.entity.City;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByCountryId(Long countryId);

    List<City> findAllByOrderByCityNameAsc();

    List<City> findByCountryIdOrderByCityNameAsc(Long countryId);
}
