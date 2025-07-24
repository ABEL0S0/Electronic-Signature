package com.ES.Backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;


import com.ES.Backend.entity.DocumentMetadata;
import com.ES.Backend.repository.DocumentMetadataRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentMetadataService {

    private final DocumentMetadataRepository repository;

    @Value("${upload.path}")
    private String uploadPath;

    public DocumentMetadataService(DocumentMetadataRepository repository) {
        this.repository = repository;
    }

    public DocumentMetadata saveDocument(MultipartFile file, String user) throws IOException {
        String rootPath = Paths.get("files").toAbsolutePath().toString();
        String fileName = file.getOriginalFilename();
        String fullPath = rootPath + File.separator + uploadPath + File.separator + fileName;


        File destinationFile = new File(fullPath);
        destinationFile.getParentFile().mkdirs(); // crea carpetas si no existen
        file.transferTo(destinationFile);


        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setuser(user);
        metadata.setFileName(fileName);
        metadata.setFilePath(fullPath);
        metadata.setUploadedAt(LocalDateTime.now());

        return repository.save(metadata);
    }

    public List<DocumentMetadata> getDocumentsByUser(String user) {
        return repository.findByuser(user);
    }

    public void deleteDocumentsByUser(String user) {
        List<DocumentMetadata> documents = repository.findByuser(user);
        for (DocumentMetadata metadata : documents) {
            try {
                Files.deleteIfExists(Paths.get(metadata.getFilePath()));
            } catch (IOException e) {
                System.err.println("Error deleting file: " + metadata.getFilePath() + " - " + e.getMessage());
            }
        }
        repository.deleteByuser(user);
    }

    public void deleteDocument(String id) throws IOException {
        DocumentMetadata metadata = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document with id " + id + " not found."));
        
        Files.deleteIfExists(Paths.get(metadata.getFilePath()));
        repository.deleteById(id);
    }

    public DocumentMetadata downloadDocument(String id) {
        DocumentMetadata metadata = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Documento no encontrado"));
        return metadata;
    }

    public Resource loadPdfById(String id) {
        DocumentMetadata metadata = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Documento no encontrado con ID: " + id));

        Path filePath = Paths.get(metadata.getFilePath())
                             .toAbsolutePath()
                             .normalize();

        Resource resource = new FileSystemResource(filePath.toFile());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se puede leer el archivo PDF: " + metadata.getFileName());
        }

        return resource;
    }    

    public void updateDocument(DocumentMetadata metadata) {
        repository.save(metadata);
    }
}