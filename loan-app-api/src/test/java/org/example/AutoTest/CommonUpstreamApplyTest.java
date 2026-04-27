package org.example.AutoTest;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CommonUpstreamApplyTest {

    public static void main(String[] args) throws Exception {
        String url = "http://localhost:8082/loan-app/api/upstream/common/institution/apply";
        String orgCode = "TestD";
        String appKey = "5A1EC3C18F0816C5";

        String phone = "18312376115";
        String name = "\u5f20\u4e09";
        String city = "\u4e0a\u6d77\u5e02";
        String cityCode = "310100";
        String province = "\u4e0a\u6d77\u5e02";
        String provinceCode = "310000";
        String workCity = "\u4e0a\u6d77\u5e02";

        JSONObject payload = new JSONObject();
        payload.put("name", name);
        payload.put("phone", phone);
        payload.put("phoneMd5", DigestUtils.md5Hex(phone));
        payload.put("age", 35);
        payload.put("city", city);
        payload.put("cityCode", cityCode);
        payload.put("province", province);
        payload.put("provinceCode", provinceCode);
        payload.put("workCity", workCity);
        payload.put("gender", 1);
        payload.put("loanTime", 12);
        payload.put("profession", 1);
        payload.put("zhima", 680);
        payload.put("providentFund", 2);
        payload.put("socialSecurity", 2);
        payload.put("commercialInsurance", 1);
        payload.put("house", 1);
        payload.put("vehicle", 2);
        payload.put("overdue", 1);
        payload.put("loanAmount", 80000);
        payload.put("deviceIp", "127.0.0.1");

        String plainJson = payload.toJSONString();
        String encryptedData = encryptEcb(plainJson, appKey);

        JSONObject requestBody = new JSONObject();
        requestBody.put("orgCode", orgCode);
        requestBody.put("data", encryptedData);

        System.out.println("URL:");
        System.out.println(url);
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
}
