package com.onlineexamsystem.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.bouncycastle.crypto.generators.SCrypt;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {

    private static final String SALT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (storedHash.startsWith("scrypt:")) {
            return matchesScrypt(rawPassword, storedHash);
        }
        if (storedHash.startsWith("pbkdf2:")) {
            return matchesPbkdf2(rawPassword, storedHash);
        }
        return false;
    }

    public String generate(String rawPassword) {
        String salt = randomSalt(16);
        int n = 32768;
        int r = 8;
        int p = 1;
        byte[] derived = SCrypt.generate(
            rawPassword.getBytes(StandardCharsets.UTF_8),
            salt.getBytes(StandardCharsets.UTF_8),
            n,
            r,
            p,
            64
        );
        return "scrypt:%d:%d:%d$%s$%s".formatted(n, r, p, salt, HexFormat.of().formatHex(derived));
    }

    private boolean matchesScrypt(String rawPassword, String storedHash) {
        String[] parts = storedHash.split("\\$");
        if (parts.length != 3) {
            return false;
        }
        String[] params = parts[0].split(":");
        if (params.length != 4) {
            return false;
        }
        int n = Integer.parseInt(params[1]);
        int r = Integer.parseInt(params[2]);
        int p = Integer.parseInt(params[3]);
        String salt = parts[1];
        byte[] expected = HexFormat.of().parseHex(parts[2]);
        byte[] actual = SCrypt.generate(
            rawPassword.getBytes(StandardCharsets.UTF_8),
            salt.getBytes(StandardCharsets.UTF_8),
            n,
            r,
            p,
            expected.length
        );
        return MessageDigest.isEqual(expected, actual);
    }

    private boolean matchesPbkdf2(String rawPassword, String storedHash) {
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 3) {
                return false;
            }
            String[] params = parts[0].split(":");
            if (params.length < 3) {
                return false;
            }
            String algorithm = switch (params[1]) {
                case "sha1" -> "PBKDF2WithHmacSHA1";
                case "sha512" -> "PBKDF2WithHmacSHA512";
                default -> "PBKDF2WithHmacSHA256";
            };
            int iterations = Integer.parseInt(params[2]);
            String salt = parts[1];
            byte[] expected = HexFormat.of().parseHex(parts[2]);
            KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, expected.length * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            byte[] actual = factory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }

    private String randomSalt(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(SALT_CHARS.length());
            builder.append(SALT_CHARS.charAt(index));
        }
        return builder.toString();
    }
}
