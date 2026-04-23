package com.example.customermanagement.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    @Size(max = 50)
    private String nic;

    @Valid
    private List<@NotBlank @Size(max = 20) String> mobileNumbers = new ArrayList<>();

    @Valid
    private List<CustomerAddressRequest> addresses = new ArrayList<>();

    private List<Long> familyMemberIds = new ArrayList<>();
}
