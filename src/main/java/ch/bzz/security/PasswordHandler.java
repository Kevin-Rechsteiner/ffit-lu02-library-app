package ch.bzz.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class PasswordHandler {

    private PasswordHandler() {
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        return md.digest(password.getBytes());
    }

    public static boolean verifyPassword(String inputPassword, byte[] storedHash, byte[] storedSalt)
            throws NoSuchAlgorithmException {
        byte[] hashedInput = hashPassword(inputPassword, storedSalt);
        return Arrays.equals(hashedInput, storedHash);
    }
}
