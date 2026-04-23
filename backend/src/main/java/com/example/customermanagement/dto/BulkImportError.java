package com.example.customermanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkImportError {

    private long rowNumber;
    private String message;
}
