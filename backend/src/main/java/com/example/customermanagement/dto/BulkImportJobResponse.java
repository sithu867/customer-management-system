package com.example.customermanagement.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkImportJobResponse {

    private String jobId;
    private BulkImportMode mode;
    private BulkImportJobStatus status;
    private String originalFilename;
    private String message;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private BulkImportResponse result;
}
