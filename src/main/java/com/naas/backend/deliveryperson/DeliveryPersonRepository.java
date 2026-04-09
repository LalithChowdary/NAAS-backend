package com.naas.backend.deliveryperson;

import java.util.UUID;

import com.naas.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, UUID> {

    Optional<DeliveryPerson> findByUser(User user);
    java.util.List<DeliveryPerson> findByStatus(String status);
    java.util.List<DeliveryPerson> findByStatusNot(String status);
}
