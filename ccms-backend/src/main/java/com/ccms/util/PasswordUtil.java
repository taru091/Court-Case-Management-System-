package com.ccms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {
    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte currentByte : hash) {
                builder.append(String.format("%02x", currentByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("Unable to hash password.", exception);
        }
    }

    public static boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        String trimmedStoredPassword = storedPassword.trim();
        return trimmedStoredPassword.equals(rawPassword)
                || trimmedStoredPassword.equalsIgnoreCase(hashPassword(rawPassword));
    }
}
