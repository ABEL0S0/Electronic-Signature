package com.ES.Backend.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ES.Backend.entity.Certificate;
import com.ES.Backend.repository.CertificateRepository;

import jakarta.annotation.PostConstruct;


@Service
public class CertificateService {
    private final CertificateRepository repository;
    private final CryptoService cryptoService;
    private PrivateKey caPrivateKey;
    private X509Certificate caCertificate;
    

    public CertificateService(CertificateRepository repository, CryptoService cryptoService) {
        Security.addProvider(new BouncyCastleProvider());
        this.repository = repository;
        this.cryptoService = cryptoService;
        try {
            // Generar CA en código
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(4096);
            KeyPair caKeyPair = keyGen.generateKeyPair();
            this.caPrivateKey = caKeyPair.getPrivate();
            String caEmail = "electronic.signature@gmail.com";
            String caDn = "emailAddress=" + caEmail + ", CN=ESI, OU=E-S, O=E-Signature, L=Esmeraldas, ST=Esmeraldas, C=EC";
            X500Name caSubject = new X500Name(caDn);
            BigInteger caSerial = BigInteger.valueOf(System.currentTimeMillis());
            Date notBefore = new Date();
            Date notAfter = new Date(notBefore.getTime() + (3650L * 24 * 60 * 60 * 1000L)); // 10 años
            JcaX509v3CertificateBuilder caCertBuilder = new JcaX509v3CertificateBuilder(
                caSubject, caSerial, notBefore, notAfter, caSubject, caKeyPair.getPublic()
            );
            caCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            caCertBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            ContentSigner caSigner = new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(caPrivateKey);
            X509CertificateHolder caCertHolder = caCertBuilder.build(caSigner);
            this.caCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(caCertHolder);
            System.out.println("CA generada en memoria correctamente");
        } catch (Exception e) {
            System.err.println("Error generando la CA en memoria: " + e.getMessage());
            throw new RuntimeException(e);
        }
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

            // Manejar certificados antiguos que tienen IV de 24 bytes
            if (iv.length != 16) {
                System.out.println("WARNING: IV length is " + iv.length + " bytes, expected 16. This is an old certificate format.");
                System.out.println("Attempting to use first 16 bytes of IV...");
                
                // Tomar solo los primeros 16 bytes del IV
                byte[] correctedIv = new byte[16];
                System.arraycopy(iv, 0, correctedIv, 0, 16);
                iv = correctedIv;
                System.out.println("IV corrected to 16 bytes");
            }

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

    public KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
    
    // Implementa generateUserCertificate usando BouncyCastle para firmar con tu CA
    public X509Certificate generateUserCertificate(String nombre, String correo, String organizacion, PublicKey userPublicKey) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Exportar la CA a archivos PEM en files/uploads/tmp
        try {
            java.io.File tmpDir = new java.io.File("files/uploads/tmp");
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }
            // Guardar el certificado de la CA
            java.io.File caCertFile = new java.io.File(tmpDir, "ca-cert.pem");
            try (java.io.FileWriter fw = new java.io.FileWriter(caCertFile);
                 org.bouncycastle.openssl.jcajce.JcaPEMWriter pemWriter = new org.bouncycastle.openssl.jcajce.JcaPEMWriter(fw)) {
                pemWriter.writeObject(this.caCertificate);
            }
            // Guardar la clave privada de la CA
            java.io.File caKeyFile = new java.io.File(tmpDir, "ca-key.pem");
            try (java.io.FileWriter fw = new java.io.FileWriter(caKeyFile);
                 org.bouncycastle.openssl.jcajce.JcaPEMWriter pemWriter = new org.bouncycastle.openssl.jcajce.JcaPEMWriter(fw)) {
                pemWriter.writeObject(this.caPrivateKey);
            }
        } catch (Exception e) {
            System.err.println("Error exportando la CA: " + e.getMessage());
        }

        // Ordenar el subject y el issuer para que coincidan exactamente
        String caEmail = "electronic.signature@gmail.com";
        String caDn = "emailAddress=" + caEmail + ", CN=ESI, OU=E-S, O=E-Signature, L=Esmeraldas, ST=Esmeraldas, C=EC";
        X500Name issuer = new X500Name(caDn);

        StringBuilder subjectDn = new StringBuilder();
        subjectDn.append("emailAddress=").append(correo);
        subjectDn.append(", CN=").append(nombre);
        subjectDn.append(", OU=E-S");
        subjectDn.append(", O=").append(organizacion != null ? organizacion : "E-Signature");
        subjectDn.append(", L=Esmeraldas");
        subjectDn.append(", ST=Esmeraldas");
        subjectDn.append(", C=EC");
        X500Name subject = new X500Name(subjectDn.toString());

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + (365L * 24 * 60 * 60 * 1000L)); // 1 año

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subject, userPublicKey
        );

        // Extensiones recomendadas para certificados de usuario
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        // ExtendedKeyUsage: clientAuth
        certBuilder.addExtension(Extension.extendedKeyUsage, false, new org.bouncycastle.asn1.x509.ExtendedKeyUsage(org.bouncycastle.asn1.x509.KeyPurposeId.id_kp_clientAuth));
        // SubjectAlternativeName: correo electrónico si está presente
        if (correo != null && !correo.isEmpty()) {
            org.bouncycastle.asn1.x509.GeneralName emailAlt = new org.bouncycastle.asn1.x509.GeneralName(org.bouncycastle.asn1.x509.GeneralName.rfc822Name, correo);
            org.bouncycastle.asn1.x509.GeneralNames subjectAltNames = new org.bouncycastle.asn1.x509.GeneralNames(emailAlt);
            certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        }
        
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(caPrivateKey);
        X509CertificateHolder certHolder = certBuilder.build(signer);

        X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

        // Imprimir información del certificado generado para depuración
        /*System.out.println("Certificado generado: " + cert.toString());
        System.out.println("Issuer: " + cert.getIssuerX500Principal());
        System.out.println("Subject: " + cert.getSubjectX500Principal());
        System.out.println("Serial: " + cert.getSerialNumber());
        System.out.println("Algoritmo de firma: " + cert.getSigAlgName());*/

        return cert;
    }
    
    // Implementa generarP12 para crear el archivo .p12
    public byte[] generarP12(X509Certificate cert, PrivateKey privateKey, String password) throws Exception {
        try {
            KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
            pkcs12.load(null, null);

            java.security.cert.Certificate[] chain = new java.security.cert.Certificate[] { cert, caCertificate };
            pkcs12.setKeyEntry("user-key", privateKey, password.toCharArray(), chain);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pkcs12.store(out, password.toCharArray());
            return out.toByteArray();
        } catch (Exception e) {
            System.err.println("Error generando el archivo .p12: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Implementa guardarCertificadoEnDB para guardar el archivo en la base de datos como ya lo haces
    public void guardarCertificadoEnDB(String username, byte[] p12Bytes, String password) throws Exception {
        // Generar salt y IV
        byte[] salt = SecureRandom.getInstanceStrong().generateSeed(16);
        byte[] iv = SecureRandom.getInstanceStrong().generateSeed(16);
    
        // Derivar clave usando CryptoService para consistencia
        SecretKey key = cryptoService.deriveKey(password.toCharArray(), salt);
    
        // Cifrar el .p12 usando el CryptoService para consistencia
        byte[] encryptedData = cryptoService.cipher(Cipher.ENCRYPT_MODE, key, iv).doFinal(p12Bytes);
    
        // Guardar en MongoDB
        Certificate cert = new Certificate();
        cert.setuser(username);
        cert.setFilename("firma_" + username + ".p12");
        cert.setData(encryptedData);
        cert.setSaltHex(Base64.getEncoder().encodeToString(salt));
        cert.setIvHex(Base64.getEncoder().encodeToString(iv));
    
        repository.save(cert);
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

    public void generateAndSaveCertificate(String nombre, String correo, String organizacion, String password, String username) throws Exception {
        // 1. Generar par de claves
        KeyPair keyPair = generateRSAKeyPair();

        // 2. Generar certificado X.509 firmado con tu CA
        X509Certificate cert = generateUserCertificate(nombre, correo, organizacion, keyPair.getPublic());

        // 3. Crear archivo .p12 protegido con contraseña
        byte[] p12Bytes = generarP12(cert, keyPair.getPrivate(), password);

        // 4. Guardar en la base de datos
        guardarCertificadoEnDB(username, p12Bytes, password);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private PrivateKey loadPrivateKeyFromPem(InputStream inputStream) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(inputStream))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
    
            if (object instanceof PEMKeyPair) {
                return converter.getKeyPair((PEMKeyPair) object).getPrivate();
            } else if (object instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) object);
            } else {
                throw new IllegalArgumentException("Formato de clave no soportado");
            }
        }
    }
   
}
