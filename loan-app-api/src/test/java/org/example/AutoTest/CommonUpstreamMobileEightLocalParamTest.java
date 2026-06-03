package org.example.AutoTest;

import com.alibaba.fastjson2.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class CommonUpstreamMobileEightLocalParamTest {

    public static void main(String[] args) throws Exception {
        String url = "http://localhost:8082/loan-app/api/upstream/common/masked/mobileEight/preCheck";
        String channelCode = "TestD";
        String appKey = "ED2733AFB99BF5E4";

        String phone = generateRandomPhone();
        String mobileEight = phone.substring(phone.length() - 8);

        JSONObject payload = new JSONObject();
        payload.put("requestId", "LOCAL-M8-" + System.currentTimeMillis());
        payload.put("mobileEight", mobileEight);
        payload.put("phone", phone);
        payload.put("loanAmount", randomInt(3, 20));
        payload.put("cityName", "1101");
        payload.put("ip", "127.0.0.1");
        payload.put("age", randomInt(28, 45));
        payload.put("sex", randomInt(1, 2));
        payload.put("hasHouse", randomInt(0, 1));
        payload.put("hasCar", randomInt(0, 1));
        payload.put("hasCompany", randomInt(0, 1));
        payload.put("hasInsurance", randomInt(0, 1));
        payload.put("hasSocial", randomInt(0, 1));
        payload.put("hasFund", randomInt(0, 1));
        payload.put("zmfScore", randomInt(1, 4));
        payload.put("overdue", randomInt(1, 3));

        String plainJson = payload.toJSONString();
        String encryptedData = encryptEcb(plainJson, appKey);

        JSONObject requestBody = new JSONObject();
        requestBody.put("channelCode", channelCode);
        requestBody.put("data", encryptedData);

        System.out.println("Local mobileEight preCheck URL:");
        System.out.println(url);
        System.out.println();
        System.out.println("Generated source data:");
        System.out.println("phone=" + phone);
        System.out.println("mobileEight=" + mobileEight);
        System.out.println();
        System.out.println("Plain payload:");
        System.out.println(plainJson);
        System.out.println();
        System.out.println("Encrypted request body:");
        System.out.println(requestBody.toJSONString());
    }

    public static String encryptEcb(String plainText, String key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static int randomInt(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private static String generateRandomPhone() {
        int suffix = ThreadLocalRandom.current().nextInt(0, 100_000_000);
        return "199" + String.format("%08d", suffix);
    }
}
