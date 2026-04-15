package com.jyfq.loan.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加解密工具类 (支持 CBC 和 ECB)
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES";

    private AesUtil() {
    }

    /**
     * 加密 (AES/CBC/PKCS5Padding)
     */
    public static String encrypt(String plainText, String key) {
        return encryptCBC(plainText, key, key.substring(0, 16));
    }

    public static String encryptCBC(String plainText, String key, String ivStr) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES CBC encrypt error", e);
        }
    }

    /**
     * 加密 (AES/ECB/PKCS5Padding) - 用于微银信用等渠道
     */
    public static String encryptECB(String plainText, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES ECB encrypt error", e);
        }
    }

    /**
     * 解密 (AES/CBC/PKCS5Padding)
     */
    public static String decrypt(String cipherText, String key) {
        return decryptCBC(cipherText, key, key.substring(0, 16));
    }

    public static String decryptCBC(String cipherText, String key, String ivStr) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES CBC decrypt error", e);
        }
    }

    /**
     * 解密 (AES/ECB/PKCS5Padding)
     */
    public static String decryptECB(String cipherText, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES ECB decrypt error", e);
        }
    }
}
