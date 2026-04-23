package com.example.customermanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerAddressResponse {

    private Long id;
    private String addressLine1;
    private String addressLine2;
    private Long cityId;
    private String cityName;
    private Long countryId;
    private String countryName;
}
