package com.example.customermanagement;

import com.example.customermanagement.dto.BulkImportJobResponse;
import com.example.customermanagement.dto.BulkImportJobStatus;
import com.example.customermanagement.dto.BulkImportMode;
import com.example.customermanagement.repository.CustomerRepository;
import com.example.customermanagement.service.BulkImportJobService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
class BulkImportJobServiceTests {

    @Autowired
    private BulkImportJobService bulkImportJobService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void queuesAndCompletesBulkImportJob() throws IOException, InterruptedException {
        MockMultipartFile file = buildWorkbook(new String[][]{
                {"Name", "Date of Birth", "NIC Number"},
                {"Alice", "1990-01-01", "901234567V"}
        });

        BulkImportJobResponse queued = bulkImportJobService.queueImport(file, BulkImportMode.UPSERT);
        Assertions.assertTrue(
                queued.getStatus() == BulkImportJobStatus.QUEUED || queued.getStatus() == BulkImportJobStatus.PROCESSING);

        BulkImportJobResponse finalState = waitForCompletion(queued.getJobId());

        Assertions.assertEquals(BulkImportJobStatus.COMPLETED, finalState.getStatus());
        Assertions.assertNotNull(finalState.getResult());
        Assertions.assertEquals(1, finalState.getResult().getCreatedCount());
        Assertions.assertEquals(1, customerRepository.count());
    }

    private BulkImportJobResponse waitForCompletion(String jobId) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        BulkImportJobResponse current = bulkImportJobService.getJob(jobId);

        while (Instant.now().isBefore(deadline)) {
            current = bulkImportJobService.getJob(jobId);
            if (current.getStatus() == BulkImportJobStatus.COMPLETED
                    || current.getStatus() == BulkImportJobStatus.FAILED) {
                return current;
            }
            Thread.sleep(100);
        }

        Assertions.fail("Timed out waiting for bulk import job to finish.");
        return current;
    }

    private MockMultipartFile buildWorkbook(String[][] rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Customers");

            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                for (int cellIndex = 0; cellIndex < rows[rowIndex].length; cellIndex++) {
                    row.createCell(cellIndex).setCellValue(rows[rowIndex][cellIndex]);
                }
            }

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "customers.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }
}
