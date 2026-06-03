package com.jyfq.loan.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.mapper.CityConfigMapper;
import com.jyfq.loan.model.entity.CityConfig;
import com.jyfq.loan.model.vo.H5IpCityVO;
import com.jyfq.loan.service.H5LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resolves public H5 user city from IP and maps it to configured cities.
 */
@Service
@RequiredArgsConstructor
public class H5LocationServiceImpl implements H5LocationService {

    private static final Integer STATUS_ENABLED = 1;
    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:]+$");
    private static final List<String> DIRECT_CITIES = List.of("北京市", "天津市", "上海市", "重庆市");

    private final CityConfigMapper cityConfigMapper;

    @Value("${h5.location.provider-url:https://whois.pconline.com.cn/ipJson.jsp?json=true&ip={ip}}")
    private String providerUrl;

    @Value("${h5.location.timeout-ms:1500}")
    private int timeoutMs;

    @Override
    public H5IpCityVO resolveIpCity(String ip) {
        String normalizedIp = trimToNull(ip);
        if (!isValidIp(normalizedIp) || isLocalIp(normalizedIp)) {
            return empty(normalizedIp, "LOCAL");
        }

        try {
            JSONObject payload = requestLocation(normalizedIp);
            String provinceName = trimToNull(firstText(payload, "pro", "province", "provinceName", "region"));
            String cityName = normalizeDirectCity(provinceName,
                    trimToNull(firstText(payload, "city", "cityName")));
            String cityCode = normalizeCityCode(firstText(payload, "cityCode", "adcode", "cid"));

            CityConfig cityConfig = findConfiguredCity(cityCode, cityName);
            if (cityConfig != null) {
                return of(normalizedIp, cityConfig.getProvinceName(), cityConfig.getCityCode(),
                        cityConfig.getCityName(), true, "IP");
            }
            if (StringUtils.hasText(cityName)) {
                return of(normalizedIp, provinceName, cityCode, cityName, true, "IP");
            }
        } catch (Exception ignored) {
            // H5 city recommendation must not block the form when IP lookup is unavailable.
        }
        return empty(normalizedIp, "IP");
    }

    private JSONObject requestLocation(String ip) {
        String url = providerUrl.replace("{ip}", URLEncoder.encode(ip, StandardCharsets.UTF_8));
        String body = HttpRequest.get(url)
                .timeout(Math.max(timeoutMs, 500))
                .execute()
                .body();
        String json = unwrapJson(body);
        return JSON.parseObject(json);
    }

    private CityConfig findConfiguredCity(String cityCode, String cityName) {
        CityConfig byCode = findByCityCode(cityCode);
        if (byCode != null) {
            return byCode;
        }
        if (!StringUtils.hasText(cityName)) {
            return null;
        }
        CityConfig exact = cityConfigMapper.selectOne(new LambdaQueryWrapper<CityConfig>()
                .eq(CityConfig::getStatus, STATUS_ENABLED)
                .eq(CityConfig::getCityName, cityName)
                .last("LIMIT 1"));
        if (exact != null) {
            return exact;
        }
        String normalizedName = normalizeCityName(cityName);
        return cityConfigMapper.selectList(new LambdaQueryWrapper<CityConfig>()
                        .eq(CityConfig::getStatus, STATUS_ENABLED)
                        .orderByAsc(CityConfig::getSort)
                        .orderByAsc(CityConfig::getCityCode))
                .stream()
                .filter(city -> normalizeCityName(city.getCityName()).equals(normalizedName))
                .findFirst()
                .orElse(null);
    }

    private CityConfig findByCityCode(String cityCode) {
        if (!StringUtils.hasText(cityCode)) {
            return null;
        }
        return cityConfigMapper.selectOne(new LambdaQueryWrapper<CityConfig>()
                .eq(CityConfig::getStatus, STATUS_ENABLED)
                .eq(CityConfig::getCityCode, cityCode)
                .last("LIMIT 1"));
    }

    private String unwrapJson(String body) {
        String value = trimToNull(body);
        if (value == null) {
            return "{}";
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return value;
    }

    private String firstText(JSONObject payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            String value = payload.getString(key);
            if (StringUtils.hasText(value) && !"0".equals(value.trim())) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeDirectCity(String provinceName, String cityName) {
        if (StringUtils.hasText(cityName)) {
            return cityName;
        }
        if (DIRECT_CITIES.contains(provinceName)) {
            return provinceName;
        }
        return null;
    }

    private String normalizeCityCode(String value) {
        String code = trimToNull(value);
        if (code == null || "0".equals(code)) {
            return null;
        }
        String digits = code.chars()
                .filter(Character::isDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (digits.length() >= 6) {
            return toCityLevelCode(digits.substring(0, 6));
        }
        return digits.length() >= 4 ? digits.substring(0, 4) : null;
    }

    private String toCityLevelCode(String code) {
        if (code.length() < 6 || "90".equals(code.substring(2, 4))) {
            return code;
        }
        return code.substring(0, 4) + "00";
    }

    private String normalizeCityName(String value) {
        String cityName = trimToNull(value);
        if (cityName == null) {
            return "";
        }
        String normalized = cityName.replace(" ", "").toUpperCase(Locale.ROOT);
        return normalized
                .replace("自治州", "")
                .replace("地区", "")
                .replace("盟", "")
                .replace("市", "");
    }

    private boolean isValidIp(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        if (IPV4.matcher(value).matches()) {
            return true;
        }
        if (!value.contains(":") || !IPV6_CHARS.matcher(value).matches()) {
            return false;
        }
        try {
            return InetAddress.getByName(value).getHostAddress().contains(":");
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isLocalIp(String value) {
        try {
            InetAddress address = InetAddress.getByName(value);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress();
        } catch (Exception ex) {
            return true;
        }
    }

    private H5IpCityVO empty(String ip, String source) {
        return of(ip, null, null, null, false, source);
    }

    private H5IpCityVO of(String ip, String provinceName, String cityCode, String cityName,
                         boolean located, String source) {
        H5IpCityVO vo = new H5IpCityVO();
        vo.setIp(ip);
        vo.setProvinceName(provinceName);
        vo.setCityCode(cityCode);
        vo.setCityName(cityName);
        vo.setLocated(located);
        vo.setSource(source);
        return vo;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
