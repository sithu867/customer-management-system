package com.example.customermanagement.service;

import com.example.customermanagement.dto.BulkImportError;
import com.example.customermanagement.dto.BulkImportMode;
import com.example.customermanagement.dto.BulkImportResponse;
import com.example.customermanagement.repository.BulkCustomerJdbcRepository;
import com.example.customermanagement.repository.CustomerRepository;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.parsers.SAXParserFactory;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

@Service
public class BulkCustomerImportService {

    private static final int MAX_ERROR_SAMPLES = 50;
    private static final List<DateTimeFormatter> SUPPORTED_DATE_FORMATS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
    );

    private final CustomerRepository customerRepository;
    private final BulkCustomerJdbcRepository bulkCustomerJdbcRepository;
    private final int batchSize;

    public BulkCustomerImportService(CustomerRepository customerRepository,
                                     BulkCustomerJdbcRepository bulkCustomerJdbcRepository,
                                     @Value("${app.bulk-import.batch-size:2000}") int batchSize) {
        this.customerRepository = customerRepository;
        this.bulkCustomerJdbcRepository = bulkCustomerJdbcRepository;
        this.batchSize = batchSize;
    }

    public BulkImportResponse importCustomers(MultipartFile file, BulkImportMode mode) {
        validateFile(file);
        return importCustomers(file.getOriginalFilename(), mode, () -> OPCPackage.open(file.getInputStream()));
    }

    public BulkImportResponse importCustomers(Path filePath, String originalFilename, BulkImportMode mode) {
        validateFilename(originalFilename);
        return importCustomers(originalFilename, mode, () -> OPCPackage.open(filePath.toFile()));
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a non-empty .xlsx file.");
        }
        validateFilename(file.getOriginalFilename());
    }

    private BulkImportResponse importCustomers(String originalFilename,
                                               BulkImportMode mode,
                                               ExcelPackageSupplier packageSupplier) {
        ImportAccumulator accumulator = new ImportAccumulator(mode);
        List<ParsedCustomerRow> batch = new ArrayList<>(batchSize);

        try (OPCPackage opcPackage = packageSupplier.open()) {
            XSSFReader reader = new XSSFReader(opcPackage);
            StylesTable stylesTable = reader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) reader.getSheetsData();

            if (!sheetIterator.hasNext()) {
                throw new IllegalArgumentException("Excel file does not contain any worksheets.");
            }

            try (InputStream sheetInputStream = sheetIterator.next()) {
                processSheet(stylesTable, strings, sheetInputStream, row -> {
                    batch.add(row);
                    if (batch.size() >= batchSize) {
                        flushBatch(batch, accumulator);
                        batch.clear();
                    }
                }, accumulator);
            }

            if (!batch.isEmpty()) {
                flushBatch(batch, accumulator);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to process the Excel file. Upload a valid .xlsx file.", ex);
        }

        return accumulator.toResponse();
    }

    private void validateFilename(String originalFilename) {
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ENGLISH).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx Excel files are supported for bulk import.");
        }
    }

    private void processSheet(StylesTable stylesTable,
                              ReadOnlySharedStringsTable strings,
                              InputStream sheetInputStream,
                              Consumer<ParsedCustomerRow> rowConsumer,
                              ImportAccumulator accumulator) throws Exception {
        DataFormatter dataFormatter = new DataFormatter();
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        XMLReader parser = saxParserFactory.newSAXParser().getXMLReader();

        parser.setContentHandler(new XSSFSheetXMLHandler(
                stylesTable,
                null,
                strings,
                new ExcelSheetHandler(rowConsumer, accumulator),
                dataFormatter,
                false));
        parser.parse(new InputSource(sheetInputStream));
    }

    private void flushBatch(List<ParsedCustomerRow> rows, ImportAccumulator accumulator) {
        accumulator.totalRows += rows.size();

        List<ParsedCustomerRow> validRows = new ArrayList<>(rows.size());
        Set<String> seenNicsInBatch = new LinkedHashSet<>();
        for (ParsedCustomerRow row : rows) {
            if (!seenNicsInBatch.add(row.getNic())) {
                accumulator.invalidCount++;
                accumulator.addError(row.getRowNumber(), "Duplicate NIC found in the same batch: " + row.getNic());
                continue;
            }
            validRows.add(row);
        }

        Map<String, CustomerRepository.CustomerNicProjection> existingByNic = customerRepository.findByNicIn(
                        validRows.stream().map(ParsedCustomerRow::getNic).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(CustomerRepository.CustomerNicProjection::getNic, projection -> projection));

        List<BulkCustomerJdbcRepository.BulkCustomerRecord> inserts = new ArrayList<>();
        List<BulkCustomerJdbcRepository.BulkCustomerUpdateRecord> updates = new ArrayList<>();

        for (ParsedCustomerRow row : validRows) {
            CustomerRepository.CustomerNicProjection existing = existingByNic.get(row.getNic());
            if (existing == null) {
                inserts.add(new BulkCustomerJdbcRepository.BulkCustomerRecord(row.getName(), row.getDateOfBirth(), row.getNic()));
                accumulator.createdCount++;
            } else if (accumulator.mode == BulkImportMode.UPSERT) {
                updates.add(new BulkCustomerJdbcRepository.BulkCustomerUpdateRecord(existing.getId(), row.getName(), row.getDateOfBirth()));
                accumulator.updatedCount++;
            } else {
                accumulator.skippedCount++;
                accumulator.addError(row.getRowNumber(), "NIC already exists: " + row.getNic());
            }
        }

        bulkCustomerJdbcRepository.batchInsertCustomers(inserts);
        bulkCustomerJdbcRepository.batchUpdateCustomers(updates);
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : SUPPORTED_DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ex) {
                // Try the next format.
            }
        }
        throw new IllegalArgumentException("Unsupported date format: " + value);
    }

    private final class ExcelSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private static final String NAME_HEADER = "name";
        private static final String DATE_OF_BIRTH_HEADER = "dateofbirth";
        private static final String NIC_HEADER = "nicnumber";

        private final Consumer<ParsedCustomerRow> rowConsumer;
        private final ImportAccumulator accumulator;
        private final Map<Integer, String> currentRow = new HashMap<>();
        private final Map<String, Integer> headerIndexes = new HashMap<>();
        private boolean headerProcessed;
        private int rowNumber;

        private ExcelSheetHandler(Consumer<ParsedCustomerRow> rowConsumer, ImportAccumulator accumulator) {
            this.rowConsumer = rowConsumer;
            this.accumulator = accumulator;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow.clear();
            rowNumber = rowNum + 1;
        }

        @Override
        public void endRow(int rowNum) {
            if (currentRow.isEmpty()) {
                return;
            }

            if (!headerProcessed) {
                headerProcessed = true;
                mapHeaders();
                return;
            }

            try {
                rowConsumer.accept(parseCurrentRow());
            } catch (IllegalArgumentException ex) {
                accumulator.invalidCount++;
                accumulator.addError(rowNumber, ex.getMessage());
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int columnIndex = new CellReference(cellReference).getCol();
            currentRow.put(columnIndex, formattedValue == null ? "" : formattedValue.trim());
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // Header and footer content is irrelevant for customer row imports.
        }

        private void mapHeaders() {
            for (Map.Entry<Integer, String> entry : currentRow.entrySet()) {
                headerIndexes.put(normalize(entry.getValue()), entry.getKey());
            }
            if (!headerIndexes.containsKey(NAME_HEADER)
                    || !headerIndexes.containsKey(DATE_OF_BIRTH_HEADER)
                    || !headerIndexes.containsKey(NIC_HEADER)) {
                throw new IllegalArgumentException(
                        "Header row must contain Name, Date of Birth, and NIC Number columns.");
            }
        }

        private ParsedCustomerRow parseCurrentRow() {
            String name = readRequired(NAME_HEADER, "Name");
            String nic = readRequired(NIC_HEADER, "NIC Number");
            LocalDate dateOfBirth = parseDate(readRequired(DATE_OF_BIRTH_HEADER, "Date of Birth"));
            return new ParsedCustomerRow(rowNumber, name, dateOfBirth, nic);
        }

        private String readRequired(String normalizedHeader, String displayName) {
            String value = currentRow.get(headerIndexes.get(normalizedHeader));
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(displayName + " is required.");
            }
            return value.trim();
        }

        private String normalize(String value) {
            String normalized = value.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z]", "");
            if ("dob".equals(normalized) || "birthdate".equals(normalized)) {
                return DATE_OF_BIRTH_HEADER;
            }
            if ("nic".equals(normalized) || "nicno".equals(normalized)) {
                return NIC_HEADER;
            }
            return normalized;
        }
    }

    private static final class ParsedCustomerRow {
        private final long rowNumber;
        private final String name;
        private final LocalDate dateOfBirth;
        private final String nic;

        private ParsedCustomerRow(long rowNumber, String name, LocalDate dateOfBirth, String nic) {
            this.rowNumber = rowNumber;
            this.name = name;
            this.dateOfBirth = dateOfBirth;
            this.nic = nic;
        }

        public long getRowNumber() {
            return rowNumber;
        }

        public String getName() {
            return name;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        public String getNic() {
            return nic;
        }
    }

    private static final class ImportAccumulator {
        private final BulkImportMode mode;
        private long totalRows;
        private long createdCount;
        private long updatedCount;
        private long skippedCount;
        private long invalidCount;
        private final List<BulkImportError> sampleErrors = new ArrayList<>();

        private ImportAccumulator(BulkImportMode mode) {
            this.mode = mode;
        }

        private void addError(long rowNumber, String message) {
            if (sampleErrors.size() < MAX_ERROR_SAMPLES) {
                sampleErrors.add(BulkImportError.builder()
                        .rowNumber(rowNumber)
                        .message(message)
                        .build());
            }
        }

        private BulkImportResponse toResponse() {
            return BulkImportResponse.builder()
                    .mode(mode)
                    .totalRows(totalRows)
                    .createdCount(createdCount)
                    .updatedCount(updatedCount)
                    .skippedCount(skippedCount)
                    .invalidCount(invalidCount)
                    .sampleErrors(sampleErrors)
                    .build();
        }
    }

    @FunctionalInterface
    private interface ExcelPackageSupplier {
        OPCPackage open() throws Exception;
    }
}
