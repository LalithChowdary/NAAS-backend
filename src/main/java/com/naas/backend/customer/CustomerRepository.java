package com.naas.backend.customer;

import java.util.UUID;

import com.naas.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUser(User user);

    Optional<Customer> findByUserId(UUID userId);

    List<Customer> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);

    List<Customer> findAllByOrderByIdDesc();
}
