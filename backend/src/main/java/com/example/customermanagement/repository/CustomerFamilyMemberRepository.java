package com.example.customermanagement.repository;

import com.example.customermanagement.entity.CustomerFamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerFamilyMemberRepository extends JpaRepository<CustomerFamilyMember, Long> {
}
