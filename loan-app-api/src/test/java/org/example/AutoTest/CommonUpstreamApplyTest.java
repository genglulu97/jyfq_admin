package org.example.AutoTest;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class CommonUpstreamApplyTest {

    public static void main(String[] args) throws Exception {

        String preCheckUrl = "http://localhost:8082/loan-app/api/upstream/common/institution/preCheck";
        String applyUrl = "http://localhost:8082/loan-app/api/upstream/common/institution/apply";
        String orgCode = "TestD";
        String appKey = "ED2733AFB99BF5E4";
        String name = generateRandomName();
        String phone = generateRandomPhone();
        String phoneMd5 = DigestUtils.md5Hex(phone);
        int gender = randomInt(1, 2);
        IdentityInfo identityInfo = generateRandomIdentity(randomInt(28, 45), gender);
        String idCard = identityInfo.idCard();
        int age = identityInfo.age();
        String idCardPrefixFour = idCard.substring(0, 4);

        JSONObject payload = new JSONObject();
        payload.put("name", name);
        payload.put("phone", phone);
        payload.put("phoneMd5", phoneMd5);
        payload.put("idCard", idCard);
        payload.put("idCardPrefixFour", idCardPrefixFour);
        payload.put("province", "北京市");
        payload.put("provinceCode", "1100");
        payload.put("city", "北京市");
        payload.put("cityCode", "1101");
        payload.put("workCity", "北京市");
        payload.put("age", age);
        payload.put("gender", gender);
        payload.put("loanTime", 5);
        payload.put("profession", 4);
        payload.put("zhima", 3);
        payload.put("providentFund", 3);
        payload.put("socialSecurity", 3);
        payload.put("commericalInsurance", 2);
        payload.put("commercialInsurance", 2);
        payload.put("house", 1);
        payload.put("overdue", 1);
        payload.put("vehicle", 1);
        payload.put("loanAmount", 4);
        payload.put("deviceIp", "127.0.0.1");
        payload.put("requestId", "REQ" + System.currentTimeMillis());
        payload.put("agreeProtocol", "1");

        String plainJson = payload.toJSONString();
        String encryptedData = encryptEcb(plainJson, appKey);

        JSONObject preCheckRequestBody = buildRequestBody(orgCode, encryptedData);
        JSONObject applyRequestBody = buildRequestBody(orgCode, encryptedData);

        System.out.println("PreCheck URL:");
        System.out.println(preCheckUrl);
        System.out.println();
        System.out.println("Generated source data:");
        System.out.println("name=" + name);
        System.out.println("phone=" + phone);
        System.out.println("phoneMd5=" + phoneMd5);
        System.out.println("idCard=" + idCard);
        System.out.println("idCardPrefixFour=" + idCardPrefixFour);
        System.out.println("age=" + age);
        System.out.println("gender=" + gender);
        System.out.println();
        System.out.println("Plain payload:");
        System.out.println(plainJson);
        System.out.println();
        System.out.println("Encrypted preCheck request body:");
        System.out.println(preCheckRequestBody.toJSONString());
        System.out.println();
        System.out.println("Apply URL:");
        System.out.println(applyUrl);
        System.out.println();
        System.out.println("Encrypted apply request body:");
        System.out.println(applyRequestBody.toJSONString());
        System.out.println();
        System.out.println("If you need to bind a specific preCheck record, add collisionNo or localOrderNo to payload, then encrypt again.");
    }

    public static String encryptEcb(String plainText, String key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static JSONObject buildRequestBody(String orgCode, String encryptedData) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("orgCode", orgCode);
        requestBody.put("data", encryptedData);
        return requestBody;
    }

    public static String generateRandomName() {
        String[] surnames = {
                "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈",
                "褚", "卫", "蒋", "沈", "韩", "杨", "朱", "秦", "许", "何",
                "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏", "陶"
        };
        String[] givenNames = {
                "一鸣", "子涵", "宇轩", "浩然", "思远", "嘉怡", "梓萱", "若曦", "明哲", "俊杰",
                "雨桐", "欣怡", "博文", "雅婷", "承泽", "语嫣", "泽宇", "诗涵", "景行", "沐阳"
        };
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return surnames[random.nextInt(surnames.length)] + givenNames[random.nextInt(givenNames.length)];
    }

    public static int randomInt(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    public static String generateRandomPhone() {
        String[] prefixes = {
                "130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
                "145", "146", "147", "148", "149",
                "150", "151", "152", "153", "155", "156", "157", "158", "159",
                "165", "166", "167",
                "170", "171", "172", "173", "174", "175", "176", "177", "178",
                "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
                "190", "191", "192", "193", "195", "196", "197", "198", "199"
        };
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String prefix = prefixes[random.nextInt(prefixes.length)];
        int suffix = random.nextInt(0, 100_000_000);
        return prefix + String.format("%08d", suffix);
    }

    private static IdentityInfo generateRandomIdentity(int targetAge, int gender) {
        String areaCode = "110101";
        LocalDate today = LocalDate.now();
        LocalDate earliestBirthDate = today.minusYears(targetAge + 1L).plusDays(1);
        LocalDate latestBirthDate = today.minusYears(targetAge);
        long days = ChronoUnit.DAYS.between(earliestBirthDate, latestBirthDate);
        LocalDate birthDate = earliestBirthDate.plusDays(ThreadLocalRandom.current().nextLong(days + 1));
        String birthDateText = String.format("%04d%02d%02d",
                birthDate.getYear(), birthDate.getMonthValue(), birthDate.getDayOfMonth());
        int sequence = generateSequenceForGender(gender);
        String first17 = areaCode + birthDateText + sequence;
        return new IdentityInfo(first17 + calculateIdCardCheckCode(first17), Period.between(birthDate, today).getYears());
    }

    private static int generateSequenceForGender(int gender) {
        int sequence = ThreadLocalRandom.current().nextInt(100, 1000);
        boolean male = gender == 1;
        if ((sequence % 2 == 1) != male) {
            sequence = sequence == 999 ? sequence - 1 : sequence + 1;
        }
        return sequence;
    }

    private record IdentityInfo(String idCard, int age) {
    }

    public static String generateRandomIdCard() {
        int gender = randomInt(1, 2);
        int targetAge = randomInt(28, 45);
        return generateRandomIdentity(targetAge, gender).idCard();
    }

    private static char calculateIdCardCheckCode(String first17) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < first17.length(); i++) {
            sum += Character.digit(first17.charAt(i), 10) * weights[i];
        }
        return checkCodes[sum % 11];
    }
}
