package com.ES.Backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ES.Backend.entity.DocumentMetadata;

import java.util.List;

public interface DocumentMetadataRepository extends MongoRepository<DocumentMetadata, String> {
    List<DocumentMetadata> findByUserUuid(String userUuid);
}

