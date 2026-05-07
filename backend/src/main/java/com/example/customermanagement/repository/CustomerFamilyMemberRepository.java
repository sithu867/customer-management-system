package com.example.customermanagement.repository;

import com.example.customermanagement.entity.CustomerFamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerFamilyMemberRepository extends JpaRepository<CustomerFamilyMember, Long> {

    @Modifying
    @Query("delete from CustomerFamilyMember fm " +
            "where fm.customer.id = :customerId or fm.familyMemberCustomer.id = :customerId")
    void deleteLinksForCustomer(@Param("customerId") Long customerId);
}
