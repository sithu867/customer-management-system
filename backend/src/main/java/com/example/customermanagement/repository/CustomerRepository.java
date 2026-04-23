package com.example.customermanagement.repository;

import com.example.customermanagement.entity.Customer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    interface CustomerNicProjection {
        Long getId();
        String getNic();
    }

    boolean existsByNic(String nic);

    boolean existsByNicAndIdNot(String nic, Long id);

    @Query("select distinct c from Customer c " +
            "left join fetch c.mobiles " +
            "left join fetch c.addresses a " +
            "left join fetch a.city " +
            "left join fetch a.country " +
            "left join fetch c.familyMembers fm " +
            "left join fetch fm.familyMemberCustomer " +
            "order by c.id desc")
    List<Customer> findAllDetailed();

    @EntityGraph(attributePaths = {"mobiles", "addresses", "addresses.city", "addresses.country", "familyMembers", "familyMembers.familyMemberCustomer"})
    Optional<Customer> findDetailedById(Long id);

    List<CustomerNicProjection> findByNicIn(Collection<String> nics);
}
