package com.ES.Backend.repository;

import com.ES.Backend.entity.Certificate;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateRepository extends MongoRepository<Certificate, String>{
    List<Certificate> findByuser(String user);
    void deleteByuser(String user);
}