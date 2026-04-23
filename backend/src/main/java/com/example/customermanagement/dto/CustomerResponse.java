package com.example.customermanagement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerResponse {

    private Long id;
    private String name;
    private LocalDate dateOfBirth;
    private String nic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> mobileNumbers;
    private List<CustomerAddressResponse> addresses;
    private List<CustomerSummaryResponse> familyMembers;
}
