package com.naas.backend.deliveryperson;

import java.util.UUID;

import com.naas.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, UUID> {

    Optional<DeliveryPerson> findByUser(User user);

    Optional<DeliveryPerson> findByUser_Email(String email);

    List<DeliveryPerson> findByStatus(String status);

    List<DeliveryPerson> findByStatusNot(String status);

    /**
     * Returns all APPROVED delivery persons whose linked user account is active.
     * Uses JOIN FETCH to eagerly load the User entity and avoid LazyInitializationException.
     */
    @Query("SELECT dp FROM DeliveryPerson dp JOIN FETCH dp.user u WHERE LOWER(dp.status) = 'approved' AND u.active = true")
    List<DeliveryPerson> findAllApprovedAndActive();
}
