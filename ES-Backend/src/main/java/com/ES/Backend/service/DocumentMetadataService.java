package com.ES.Backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ES.Backend.entity.DocumentMetadata;
import com.ES.Backend.repository.DocumentMetadataRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentMetadataService {

    private final DocumentMetadataRepository repository;

    @Value("${upload.path}")
    private String uploadPath;

    public DocumentMetadataService(DocumentMetadataRepository repository) {
        this.repository = repository;
    }

    public DocumentMetadata saveDocument(MultipartFile file, String userUuid, String signature, String hashAlgorithm) throws IOException {
        String rootPath = Paths.get("files").toAbsolutePath().toString();
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String fullPath = rootPath + File.separator + uploadPath + File.separator + fileName;


        File destinationFile = new File(fullPath);
        destinationFile.getParentFile().mkdirs(); // crea carpetas si no existen
        file.transferTo(destinationFile);


        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setUserUuid(userUuid);
        metadata.setFileName(fileName);
        metadata.setFilePath(fullPath);
        metadata.setSignature(signature);
        metadata.setHashAlgorithm(hashAlgorithm);
        metadata.setUploadedAt(LocalDateTime.now());

        return repository.save(metadata);
    }
}
