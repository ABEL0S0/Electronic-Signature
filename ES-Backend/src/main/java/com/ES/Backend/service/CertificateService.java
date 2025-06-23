package com.ES.Backend.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ES.Backend.entity.Certificate;
import com.ES.Backend.repository.CertificateRepository;

@Service
public class CertificateService {
    private final CertificateRepository repository;
    private final CryptoService cryptoService;

    public CertificateService(CertificateRepository repository, CryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    public String encrypt(String user, MultipartFile file, char[] password) throws Exception {
        System.out.println("Encrypting file: " + file.getOriginalFilename() + " for user: " + user);
        
        byte[] salt = new SecureRandom().generateSeed(16);
        SecretKey key = cryptoService.deriveKey(password, salt);
        byte[] iv = new SecureRandom().generateSeed(16);
        byte[] encrypted = cryptoService.cipher(Cipher.ENCRYPT_MODE, key, iv).doFinal(file.getBytes());

        Certificate entity = new Certificate();
        entity.setuser(user);
        entity.setFilename(file.getOriginalFilename());
        entity.setData(encrypted);
        entity.setSaltHex(Base64.getEncoder().encodeToString(salt));
        entity.setIvHex(Base64.getEncoder().encodeToString(iv));
        
        Certificate saved = repository.save(entity);
        System.out.println("Certificate encrypted and saved with ID: " + saved.getId());
        return saved.getId();
    }

    public byte[] decrypt(String id, char[] password) throws Exception {
        System.out.println("Attempting to decrypt certificate with ID: " + id);
        
        Certificate entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Certificado no encontrado"));

        System.out.println("Found certificate: " + entity.getFilename() + " for user: " + entity.getuser());

        try {
            byte[] salt = Base64.getDecoder().decode(entity.getSaltHex());
            System.out.println("Salt decoded successfully, length: " + salt.length);
            
            SecretKey key = cryptoService.deriveKey(password, salt);
            System.out.println("Key derived successfully");
            
            byte[] iv = Base64.getDecoder().decode(entity.getIvHex());
            System.out.println("IV decoded successfully, length: " + iv.length);

            byte[] decrypted = cryptoService.cipher(Cipher.DECRYPT_MODE, key, iv).doFinal(entity.getData());
            System.out.println("Certificate decrypted successfully, size: " + decrypted.length + " bytes");
            return decrypted;
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
            System.err.println("Password length: " + password.length);
            System.err.println("Salt hex: " + entity.getSaltHex());
            System.err.println("IV hex: " + entity.getIvHex());
            System.err.println("Data length: " + entity.getData().length);
            throw e;
        }
    }

    public List<Certificate> getCertificatesByUser(String user) {
        return repository.findByuser(user);
    }

    public void deleteCertificatesByUser(String user) {
        repository.deleteByuser(user);
    }

    public Optional<Certificate> getCertificateById(String id) {
        return repository.findById(id);
    }
    
    public void deleteCertificate(String id) {
        repository.deleteById(id);
    }
}
