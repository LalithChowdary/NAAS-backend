package com.naas.backend.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalDeliveryPauseRepository extends JpaRepository<GlobalDeliveryPause, Long> {
    List<GlobalDeliveryPause> findByCustomerId(Long customerId);
}
