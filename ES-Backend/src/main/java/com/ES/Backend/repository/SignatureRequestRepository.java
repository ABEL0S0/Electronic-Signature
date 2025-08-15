package com.ES.Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ES.Backend.entity.SignatureRequest;

public interface SignatureRequestRepository extends JpaRepository<SignatureRequest, Long> {
    // Métodos personalizados si se requieren
}
