package com.ES.Backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ES.Backend.entity.DocumentMetadata;

import java.util.List;

@Repository
public interface DocumentMetadataRepository extends MongoRepository<DocumentMetadata, String> {
    List<DocumentMetadata> findByuser(String user);
    void deleteByuser(String user);
}

