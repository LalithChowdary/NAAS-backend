package com.naas.backend.deliveryperson;

import com.naas.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Long> {

    Optional<DeliveryPerson> findByUser(User user);
}
