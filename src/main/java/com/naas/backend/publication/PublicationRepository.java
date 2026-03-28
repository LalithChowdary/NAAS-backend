package com.naas.backend.publication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {
    List<Publication> findAllByOrderByIdDesc();
    List<Publication> findByNameContainingIgnoreCaseOrderByIdDesc(String name);
    List<Publication> findByEnabledOrderByIdDesc(boolean enabled);
    List<Publication> findByNameContainingIgnoreCaseAndEnabledOrderByIdDesc(String name, boolean enabled);
}
