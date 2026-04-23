package com.example.customermanagement.service;

import com.example.customermanagement.dto.BulkImportJobResponse;
import com.example.customermanagement.dto.BulkImportJobStatus;
import com.example.customermanagement.dto.BulkImportMode;
import com.example.customermanagement.dto.BulkImportResponse;
import com.example.customermanagement.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BulkImportJobService {

    private final BulkCustomerImportService bulkCustomerImportService;
    private final Executor bulkImportTaskExecutor;
    private final Map<String, BulkImportJobState> jobs = new ConcurrentHashMap<>();

    public BulkImportJobService(BulkCustomerImportService bulkCustomerImportService,
                                @Qualifier("bulkImportTaskExecutor") Executor bulkImportTaskExecutor) {
        this.bulkCustomerImportService = bulkCustomerImportService;
        this.bulkImportTaskExecutor = bulkImportTaskExecutor;
    }

    public BulkImportJobResponse queueImport(MultipartFile file, BulkImportMode mode) {
        bulkCustomerImportService.validateFile(file);

        String jobId = UUID.randomUUID().toString();
        Path tempFile = persistUpload(jobId, file);
        BulkImportJobState jobState = BulkImportJobState.queued(jobId, mode, file.getOriginalFilename(), tempFile);
        jobs.put(jobId, jobState);

        bulkImportTaskExecutor.execute(() -> processJob(jobState));
        return jobState.toResponse();
    }

    public BulkImportJobResponse getJob(String jobId) {
        BulkImportJobState jobState = jobs.get(jobId);
        if (jobState == null) {
            throw new ResourceNotFoundException("Bulk import job not found for id " + jobId);
        }
        return jobState.toResponse();
    }

    private void processJob(BulkImportJobState jobState) {
        jobState.markProcessing();
        try {
            BulkImportResponse result = bulkCustomerImportService.importCustomers(
                    jobState.getTempFile(),
                    jobState.getOriginalFilename(),
                    jobState.getMode());
            jobState.markCompleted(result);
        } catch (Exception ex) {
            jobState.markFailed(ex.getMessage());
        } finally {
            deleteTempFile(jobState.getTempFile());
        }
    }

    private Path persistUpload(String jobId, MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile("customer-import-" + jobId + "-", ".xlsx");
            file.transferTo(tempFile.toFile());
            return tempFile;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to store the uploaded Excel file for processing.", ex);
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ex) {
            // Best-effort cleanup. Keeping the job result is more important than surfacing temp file errors.
        }
    }

    private static final class BulkImportJobState {
        private final String jobId;
        private final BulkImportMode mode;
        private final String originalFilename;
        private final Path tempFile;
        private final Instant createdAt;
        private volatile BulkImportJobStatus status;
        private volatile String message;
        private volatile Instant startedAt;
        private volatile Instant completedAt;
        private volatile BulkImportResponse result;

        private BulkImportJobState(String jobId,
                                   BulkImportMode mode,
                                   String originalFilename,
                                   Path tempFile,
                                   Instant createdAt,
                                   BulkImportJobStatus status,
                                   String message) {
            this.jobId = jobId;
            this.mode = mode;
            this.originalFilename = originalFilename;
            this.tempFile = tempFile;
            this.createdAt = createdAt;
            this.status = status;
            this.message = message;
        }

        private static BulkImportJobState queued(String jobId, BulkImportMode mode, String originalFilename, Path tempFile) {
            return new BulkImportJobState(
                    jobId,
                    mode,
                    originalFilename,
                    tempFile,
                    Instant.now(),
                    BulkImportJobStatus.QUEUED,
                    "Upload accepted. Waiting for an import worker.");
        }

        private void markProcessing() {
            this.status = BulkImportJobStatus.PROCESSING;
            this.startedAt = Instant.now();
            this.message = "Import job is processing in the background.";
        }

        private void markCompleted(BulkImportResponse result) {
            this.status = BulkImportJobStatus.COMPLETED;
            this.completedAt = Instant.now();
            this.result = result;
            this.message = "Import job completed successfully.";
        }

        private void markFailed(String errorMessage) {
            this.status = BulkImportJobStatus.FAILED;
            this.completedAt = Instant.now();
            this.message = errorMessage == null || errorMessage.trim().isEmpty()
                    ? "Import job failed."
                    : errorMessage;
        }

        private BulkImportJobResponse toResponse() {
            return BulkImportJobResponse.builder()
                    .jobId(jobId)
                    .mode(mode)
                    .status(status)
                    .originalFilename(originalFilename)
                    .message(message)
                    .createdAt(createdAt)
                    .startedAt(startedAt)
                    .completedAt(completedAt)
                    .result(result)
                    .build();
        }

        private Path getTempFile() {
            return tempFile;
        }

        private String getOriginalFilename() {
            return originalFilename;
        }

        private BulkImportMode getMode() {
            return mode;
        }
    }
}
