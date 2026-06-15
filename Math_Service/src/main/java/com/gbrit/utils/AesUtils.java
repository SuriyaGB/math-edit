package com.gbrit.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

public class AesUtils {

    public static String decryptAes(String encryptedText, String key, String iv) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(Constants.TRANSFORMATION, Constants.BC);
            // Truncate or pad the IV to 16 bytes (128 bits)
            byte[] ivBytes = new byte[16];
            byte[] providedIV = iv.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(providedIV, 0, ivBytes, 0, Math.min(providedIV.length, 16));
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), Constants.AES);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            // Decrypt the text
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            // Convert the decrypted bytes to a plain text string
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encryptAes(String decryptedText, String key, String iv) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance(Constants.TRANSFORMATION, Constants.BC);
            // Truncate or pad the IV to 16 bytes (128 bits)
            byte[] ivBytes = new byte[16];
            byte[] providedIV = iv.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(providedIV, 0, ivBytes, 0, Math.min(providedIV.length, 16));
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), Constants.AES);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            // Encrypt the text
            byte[] encryptedBytes = cipher.doFinal(decryptedText.getBytes(StandardCharsets.UTF_8));
            // Convert the encrypted bytes to a Base64-encoded string
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
