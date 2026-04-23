package com.example.customermanagement.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BulkCustomerJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public BulkCustomerJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void batchInsertCustomers(List<BulkCustomerRecord> customers) {
        if (customers.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO customer (name, date_of_birth, nic) VALUES (?, ?, ?)",
                customers,
                customers.size(),
                (ps, customer) -> {
                    ps.setString(1, customer.getName());
                    ps.setDate(2, Date.valueOf(customer.getDateOfBirth()));
                    ps.setString(3, customer.getNic());
                });
    }

    public void batchUpdateCustomers(List<BulkCustomerUpdateRecord> customers) {
        if (customers.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "UPDATE customer SET name = ?, date_of_birth = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                customers,
                customers.size(),
                (ps, customer) -> {
                    ps.setString(1, customer.getName());
                    ps.setDate(2, Date.valueOf(customer.getDateOfBirth()));
                    ps.setLong(3, customer.getId());
                });
    }

    public static class BulkCustomerRecord {
        private final String name;
        private final LocalDate dateOfBirth;
        private final String nic;

        public BulkCustomerRecord(String name, LocalDate dateOfBirth, String nic) {
            this.name = name;
            this.dateOfBirth = dateOfBirth;
            this.nic = nic;
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

    public static class BulkCustomerUpdateRecord {
        private final Long id;
        private final String name;
        private final LocalDate dateOfBirth;

        public BulkCustomerUpdateRecord(Long id, String name, LocalDate dateOfBirth) {
            this.id = id;
            this.name = name;
            this.dateOfBirth = dateOfBirth;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }
    }
}
