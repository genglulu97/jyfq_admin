package com.jyfq.loan.common.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * RSA 加解密 / 签名验签工具类
 */
public final class RsaUtil {

    private static final String ALGORITHM = "RSA";
    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    private RsaUtil() {
    }

    /**
     * 公钥加密
     */
    public static String encryptByPublicKey(String plainText, String publicKeyBase64) {
        try {
            PublicKey publicKey = loadPublicKey(publicKeyBase64);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA encrypt error", e);
        }
    }

    /**
     * 私钥解密
     */
    public static String decryptByPrivateKey(String cipherText, String privateKeyBase64) {
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyBase64);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("RSA decrypt error", e);
        }
    }

    /**
     * 私钥签名
     */
    public static String sign(String data, String privateKeyBase64) {
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyBase64);
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("RSA sign error", e);
        }
    }

    /**
     * 公钥验签
     */
    public static boolean verify(String data, String signBase64, String publicKeyBase64) {
        try {
            PublicKey publicKey = loadPublicKey(publicKeyBase64);
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            throw new RuntimeException("RSA verify error", e);
        }
    }

    private static PublicKey loadPublicKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private static PrivateKey loadPrivateKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
