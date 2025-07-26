package com.ES.Backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ES.Backend.entity.CertificateRequest;

@Repository
public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, Long> {
    List<CertificateRequest> findByUserEmail(String userEmail);
    List<CertificateRequest> findByStatus(String status);
    List<CertificateRequest> findByStatusOrderByRequestedAtDesc(String status);
} 