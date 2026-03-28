package com.naas.backend.customer;

import com.naas.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUser(User user);

    Optional<Customer> findByUserId(Long userId);

    List<Customer> findByNameContainingIgnoreCaseOrPhoneContainingOrCityContainingIgnoreCase(
            String name, String phone, String city);

    List<Customer> findAllByOrderByIdDesc();
}
