package com.example.customermanagement.dto;

import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerAddressRequest {

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    private Long cityId;

    private Long countryId;
}
