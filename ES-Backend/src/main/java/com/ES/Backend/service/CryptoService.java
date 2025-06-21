package com.ES.Backend.service;

import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.springframework.stereotype.Service;

@Service
public class CryptoService {
    private static final int ITER = 100_000;
    private static final int KEY_LEN = 256;

    public SecretKey deriveKey(char[] pwd, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(pwd, salt, ITER, KEY_LEN);
        byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    public Cipher cipher(int mode, SecretKey key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(mode, key, new IvParameterSpec(iv));
        return c;
    }
}
