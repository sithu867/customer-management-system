package com.example.customermanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CityResponse {

    private Long id;
    private String cityName;
    private Long countryId;
    private String countryName;
}
