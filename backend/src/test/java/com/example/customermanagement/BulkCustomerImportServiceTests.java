package com.example.customermanagement;

import com.example.customermanagement.dto.BulkImportMode;
import com.example.customermanagement.dto.BulkImportResponse;
import com.example.customermanagement.entity.Customer;
import com.example.customermanagement.repository.CustomerRepository;
import com.example.customermanagement.service.BulkCustomerImportService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
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
class BulkCustomerImportServiceTests {

    @Autowired
    private BulkCustomerImportService bulkCustomerImportService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void importsRowsFromExcel() throws IOException {
        MockMultipartFile file = buildWorkbook(new String[][]{
                {"Name", "Date of Birth", "NIC Number"},
                {"Alice", "1990-01-01", "901234567V"},
                {"Bob", "1988-12-05", "881234567V"}
        });

        BulkImportResponse response = bulkCustomerImportService.importCustomers(file, BulkImportMode.CREATE_ONLY);

        Assertions.assertEquals(2, response.getTotalRows());
        Assertions.assertEquals(2, response.getCreatedCount());
        Assertions.assertEquals(0, response.getInvalidCount());
        Assertions.assertEquals(2, customerRepository.count());
    }

    @Test
    void upsertUpdatesExistingCustomerByNic() throws IOException {
        Customer customer = new Customer();
        customer.setName("Old Name");
        customer.setDateOfBirth(LocalDate.of(1991, 3, 2));
        customer.setNic("911111111V");
        customerRepository.save(customer);

        MockMultipartFile file = buildWorkbook(new String[][]{
                {"Name", "Date of Birth", "NIC Number"},
                {"New Name", "1991-03-02", "911111111V"}
        });

        BulkImportResponse response = bulkCustomerImportService.importCustomers(file, BulkImportMode.UPSERT);
        List<Customer> customers = customerRepository.findAll();

        Assertions.assertEquals(1, response.getUpdatedCount());
        Assertions.assertEquals(1, customers.size());
        Assertions.assertEquals("New Name", customers.get(0).getName());
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
