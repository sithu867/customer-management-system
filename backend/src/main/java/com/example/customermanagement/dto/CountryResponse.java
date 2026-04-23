package com.example.customermanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CountryResponse {

    private Long id;
    private String countryName;
}
