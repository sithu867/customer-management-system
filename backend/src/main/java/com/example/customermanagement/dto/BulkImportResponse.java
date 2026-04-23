package com.example.customermanagement.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkImportResponse {

    private BulkImportMode mode;
    private long totalRows;
    private long createdCount;
    private long updatedCount;
    private long skippedCount;
    private long invalidCount;
    private List<BulkImportError> sampleErrors;
}
