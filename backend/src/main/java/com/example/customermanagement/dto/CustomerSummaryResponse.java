package com.example.customermanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerSummaryResponse {

    private Long id;
    private String name;
    private String nic;
}
