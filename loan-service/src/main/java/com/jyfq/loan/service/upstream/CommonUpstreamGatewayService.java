package com.jyfq.loan.service.upstream;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.CityConfigMapper;
import com.jyfq.loan.model.dto.CommonUpstreamEnvelopeDTO;
import com.jyfq.loan.model.dto.CommonUpstreamMobileEightEnvelopeDTO;
import com.jyfq.loan.model.dto.CommonUpstreamMobileEightPayloadDTO;
import com.jyfq.loan.model.dto.CommonUpstreamPayloadDTO;
import com.jyfq.loan.model.dto.CommonUpstreamTestRequestDTO;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.CityConfig;
import com.jyfq.loan.model.entity.CollisionRecord;
import com.jyfq.loan.service.ApplyService;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonUpstreamGatewayService {

    private static final Set<String> SUPPORTED_SCENES = Set.of("institution", "masked", "half-process", "full-process");

    private final ChannelMapper channelMapper;
    private final ApplyService applyService;
    private final ChannelCryptoService channelCryptoService;
    private final ChannelAccessGuard channelAccessGuard;
    private final CityConfigMapper cityConfigMapper;

    public Map<String, Object> preCheck(String scene, CommonUpstreamEnvelopeDTO request, String clientIp) {
        validateScene(scene);
        log.info("【公共上游预检】请求入参：{}", JSON.toJSONString(request));
        Channel channel = getEnabledChannel(request.getOrgCode());
        channelAccessGuard.validate(channel, clientIp);
        CommonUpstreamPayloadDTO payload = decryptPayload(channel, request.getData());
        validatePreCheckPayload(payload);

        StandardApplyData applyData = CommonUpstreamMappingUtil.toStandardData(channel.getChannelCode(), payload);
        fillRequestIp(applyData, clientIp);
        enrichExtraInfo(applyData, payload, scene, channel.getChannelCode());
        logParsedPayload("公共上游预检", scene, channel, payload, applyData);

        PreCheckResult winner = applyService.competitivePreCheck(applyData);
        return buildPreCheckResponse(winner);
    }

    public Map<String, Object> mobileEightPreCheck(String scene, CommonUpstreamMobileEightEnvelopeDTO request, String clientIp) {
        validateScene(scene);
        Channel channel = getEnabledChannel(request.getChannelCode());
        channelAccessGuard.validate(channel, clientIp);
        CommonUpstreamMobileEightPayloadDTO payload = decryptMobileEightPayload(channel, request.getData());
        validateMobileEightPayload(payload);

        StandardApplyData applyData = toMobileEightStandardData(channel.getChannelCode(), payload, clientIp);
        PreCheckResult winner = applyService.mobileEightPreCheck(
                applyData,
                trimToNull(payload.getRequestId()),
                resolveMobileEight(payload.getMobileEight(), payload.getPhone()));
        return buildMobileEightPreCheckResponse(winner);
    }

    public Map<String, Object> apply(String scene, CommonUpstreamEnvelopeDTO request, String clientIp) {
        validateScene(scene);
        log.info("【公共上游进件】请求入参：{}", JSON.toJSONString(request));
        Channel channel = getEnabledChannel(request.getOrgCode());
        channelAccessGuard.validate(channel, clientIp);
        CommonUpstreamPayloadDTO payload = decryptPayload(channel, request.getData());
        validateApplyPayload(payload);

        StandardApplyData applyData = CommonUpstreamMappingUtil.toStandardData(channel.getChannelCode(), payload);
        fillRequestIp(applyData, clientIp);
        enrichExtraInfo(applyData, payload, scene, channel.getChannelCode());
        logParsedPayload("公共上游进件", scene, channel, payload, applyData);

        String collisionNo = resolveCollisionNo(payload);
        CollisionRecord matchedOrder = StringUtils.hasText(collisionNo)
                ? applyService.findMatchedCollisionRecord(applyData, collisionNo)
                : applyService.findLatestMatchedCollisionRecord(applyData);
        if (matchedOrder == null || matchedOrder.getProductId() == null) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "请先preCheck并撞库通过后再apply");
        }

        validateApplyAgainstMatchedOrder(matchedOrder, payload, applyData);
        PushResult pushResult = applyService.pushToInstitution(applyData, matchedOrder.getProductId(), matchedOrder.getCollisionNo());
        return buildApplyResponse(scene, channel.getChannelCode(), matchedOrder.getProductId(), pushResult);
    }

    public Map<String, Object> testPreCheck(String scene, CommonUpstreamTestRequestDTO request, String clientIp) {
        validateScene(scene);
        Channel channel = resolveTestChannel(request == null ? null : request.getOrgCode());
        CommonUpstreamPayloadDTO payload = buildTestPayload(request, clientIp);
        validatePreCheckPayload(payload);

        StandardApplyData applyData = CommonUpstreamMappingUtil.toStandardData(channel.getChannelCode(), payload);
        fillRequestIp(applyData, clientIp);
        enrichExtraInfo(applyData, payload, scene, channel.getChannelCode());
        logParsedPayload("Common upstream test preCheck", scene, channel, payload, applyData);

        PreCheckResult winner = applyService.competitivePreCheck(applyData);
        return buildPreCheckResponse(winner);
    }

    public Map<String, Object> testApply(String scene, CommonUpstreamTestRequestDTO request, String clientIp) {
        validateScene(scene);
        Channel channel = resolveTestChannel(request == null ? null : request.getOrgCode());
        CommonUpstreamPayloadDTO payload = buildTestPayload(request, clientIp);
        validateApplyPayload(payload);

        StandardApplyData applyData = CommonUpstreamMappingUtil.toStandardData(channel.getChannelCode(), payload);
        fillRequestIp(applyData, clientIp);
        enrichExtraInfo(applyData, payload, scene, channel.getChannelCode());
        logParsedPayload("Common upstream test apply", scene, channel, payload, applyData);

        String collisionNo = resolveCollisionNo(payload);
        CollisionRecord matchedOrder = StringUtils.hasText(collisionNo)
                ? applyService.findMatchedCollisionRecord(applyData, collisionNo)
                : applyService.findLatestMatchedCollisionRecord(applyData);
        if (matchedOrder == null || matchedOrder.getProductId() == null) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "Please preCheck successfully before apply");
        }

        validateApplyAgainstMatchedOrder(matchedOrder, payload, applyData);
        Long productId = request.getProductId() == null ? matchedOrder.getProductId() : request.getProductId();
        PushResult pushResult = applyService.pushToInstitution(applyData, productId, matchedOrder.getCollisionNo());
        return buildApplyResponse(scene, channel.getChannelCode(), productId, pushResult);
    }

    public List<Map<String, Object>> testBatchPreCheck(String scene, List<CommonUpstreamTestRequestDTO> requests, String clientIp) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "request list is required");
        }
        return requests.stream()
                .map(request -> testPreCheck(scene, request, clientIp))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> testBatchApply(String scene, List<CommonUpstreamTestRequestDTO> requests, String clientIp) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "request list is required");
        }
        return requests.stream()
                .map(request -> testApply(scene, request, clientIp))
                .collect(Collectors.toList());
    }

    private void validateScene(String scene) {
        if (!SUPPORTED_SCENES.contains(scene)) {
            throw new BizException(ResultCode.PARAM_ERROR, "unsupported scene: " + scene);
        }
    }

    private Channel getEnabledChannel(String orgCode) {
        if (!StringUtils.hasText(orgCode)) {
            throw new BizException(ResultCode.PARAM_MISSING, "orgCode");
        }

        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, orgCode.trim()));
        if (channel == null) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, orgCode);
        }
        if (!Integer.valueOf(1).equals(channel.getStatus())) {
            throw new BizException(ResultCode.CHANNEL_DISABLED, orgCode);
        }
        if (!StringUtils.hasText(channel.getAppKey())) {
            throw new BizException(ResultCode.DECRYPT_ERROR, "channel appKey missing");
        }
        return channel;
    }

    private Channel resolveTestChannel(String orgCode) {
        if (StringUtils.hasText(orgCode)) {
            Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                    .eq(Channel::getChannelCode, orgCode.trim())
                    .last("LIMIT 1"));
            if (channel == null) {
                throw new BizException(ResultCode.CHANNEL_NOT_FOUND, orgCode);
            }
            if (!Integer.valueOf(1).equals(channel.getStatus())) {
                throw new BizException(ResultCode.CHANNEL_DISABLED, orgCode);
            }
            return channel;
        }

        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getId)
                .last("LIMIT 1"));
        if (channel == null) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, "enabled channel");
        }
        return channel;
    }

    private CommonUpstreamPayloadDTO buildTestPayload(CommonUpstreamTestRequestDTO request, String clientIp) {
        if (request == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "request is required");
        }
        int seed = Math.abs(java.util.Objects.hash(
                trimToNull(request.getName()),
                trimToNull(request.getPhone()),
                trimToNull(request.getIdCard()),
                trimToNull(request.getCity())));

        CommonUpstreamPayloadDTO payload = new CommonUpstreamPayloadDTO();
        payload.setName(trimToNull(request.getName()));
        payload.setPhone(trimToNull(request.getPhone()));
        payload.setPhoneMd5(CommonUpstreamMappingUtil.resolvePhoneMd5(null, request.getPhone()));
        payload.setIdCard(trimToNull(request.getIdCard()));
        payload.setIdCardPrefixFour(resolveIdCardPrefixFour(request.getIdCard()));
        payload.setAge(resolveAge(request.getIdCard(), seed));
        payload.setCity(trimToNull(request.getCity()));
        payload.setCityCode(resolveTestCityCode(request.getCityCode(), request.getCity()));
        payload.setGender(resolveGender(request.getIdCard(), seed));
        payload.setLoanTime(pick(seed, 1, 2, 3, 4, 5));
        payload.setProfession(pick(seed, 2, 1, 2, 3, 4));
        payload.setZhima(pick(seed, 3, 1, 2, 3));
        payload.setProvidentFund(pick(seed, 4, 1, 2, 3));
        payload.setSocialSecurity(pick(seed, 5, 1, 2, 3));
        payload.setCommercialInsurance(pick(seed, 6, 0, 1, 2));
        payload.setHouse(pick(seed, 7, 1, 2));
        payload.setOverdue(1);
        payload.setVehicle(pick(seed, 8, 1, 2));
        payload.setLoanAmount(pick(seed, 9, 1, 2, 3, 4));
        payload.setDeviceIp(clientIp);
        payload.setCollisionNo(trimToNull(firstText(request.getCollisionNo(), request.getLocalOrderNo())));
        payload.setLocalOrderNo(trimToNull(request.getLocalOrderNo()));
        payload.setProductId(request.getProductId());

        Map<String, Object> extraInfo = new LinkedHashMap<>();
        extraInfo.put("test", true);
        extraInfo.put("randomSeed", seed);
        payload.setExtraInfo(extraInfo);
        return payload;
    }

    private String resolveIdCardPrefixFour(String idCard) {
        String trimmed = trimToNull(idCard);
        if (trimmed == null || trimmed.length() < 4) {
            return null;
        }
        return trimmed.substring(0, 4);
    }

    private String resolveTestCityCode(String cityCode, String cityName) {
        String submittedCode = trimToNull(cityCode);
        if (submittedCode != null) {
            return submittedCode;
        }
        String submittedCity = trimToNull(cityName);
        if (submittedCity == null) {
            return null;
        }

        CityConfig exact = cityConfigMapper.selectOne(new LambdaQueryWrapper<CityConfig>()
                .eq(CityConfig::getStatus, 1)
                .eq(CityConfig::getCityName, submittedCity)
                .last("LIMIT 1"));
        if (exact != null && StringUtils.hasText(exact.getCityCode())) {
            return exact.getCityCode();
        }

        String normalizedCity = submittedCity.endsWith("市")
                ? submittedCity.substring(0, submittedCity.length() - 1)
                : submittedCity;
        CityConfig fuzzy = cityConfigMapper.selectOne(new LambdaQueryWrapper<CityConfig>()
                .eq(CityConfig::getStatus, 1)
                .likeRight(CityConfig::getCityName, normalizedCity)
                .last("LIMIT 1"));
        if (fuzzy != null && StringUtils.hasText(fuzzy.getCityCode())) {
            return fuzzy.getCityCode();
        }

        return submittedCity;
    }

    private Integer resolveAge(String idCard, int seed) {
        String trimmed = trimToNull(idCard);
        if (trimmed != null && trimmed.length() == 18) {
            try {
                LocalDate birthday = LocalDate.parse(trimmed.substring(6, 14),
                        java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                return Period.between(birthday, LocalDate.now()).getYears();
            } catch (Exception ignored) {
                // Fall through to deterministic test age.
            }
        }
        return pick(seed, 10, 22, 25, 28, 31, 35, 40, 45, 50);
    }

    private Integer resolveGender(String idCard, int seed) {
        String trimmed = trimToNull(idCard);
        if (trimmed != null && trimmed.length() == 18) {
            char genderCode = trimmed.charAt(16);
            if (Character.isDigit(genderCode)) {
                return (genderCode - '0') % 2 == 0 ? 2 : 1;
            }
        }
        return pick(seed, 11, 1, 2);
    }

    private int pick(int seed, int salt, int... values) {
        return values[Math.floorMod(seed + salt * 1103515245, values.length)];
    }

    private void fillRequestIp(StandardApplyData applyData, String clientIp) {
        if (applyData != null && !StringUtils.hasText(applyData.getIp())) {
            applyData.setIp(clientIp);
        }
    }

    private CommonUpstreamPayloadDTO decryptPayload(Channel channel, String encryptedData) {
        if (!StringUtils.hasText(encryptedData)) {
            throw new BizException(ResultCode.PARAM_MISSING, "data");
        }
        try {
            String decrypted = channelCryptoService.decrypt(channel, encryptedData);
            CommonUpstreamPayloadDTO payload = JSON.parseObject(decrypted, CommonUpstreamPayloadDTO.class);
            if (payload == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "empty payload");
            }
            return payload;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[COMMON-UPSTREAM] decrypt failed, orgCode={}", channel.getChannelCode(), ex);
            throw new BizException(ResultCode.DECRYPT_ERROR, "AES/ECB decrypt failed");
        }
    }

    private CommonUpstreamMobileEightPayloadDTO decryptMobileEightPayload(Channel channel, String encryptedData) {
        if (!StringUtils.hasText(encryptedData)) {
            throw new BizException(ResultCode.PARAM_MISSING, "data");
        }
        try {
            String decrypted = channelCryptoService.decrypt(channel, encryptedData);
            CommonUpstreamMobileEightPayloadDTO payload = JSON.parseObject(decrypted, CommonUpstreamMobileEightPayloadDTO.class);
            if (payload == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "empty payload");
            }
            return payload;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[COMMON-UPSTREAM] mobileEight decrypt failed, channelCode={}", channel.getChannelCode(), ex);
            throw new BizException(ResultCode.DECRYPT_ERROR, "AES/ECB decrypt failed");
        }
    }

    private void validateMobileEightPayload(CommonUpstreamMobileEightPayloadDTO payload) {
        if (payload == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "payload is required");
        }
        requireText(payload.getRequestId(), "requestId");
        requireText(payload.getPhone(), "phone");
        String mobileEight = resolveMobileEight(payload.getMobileEight(), payload.getPhone());
        requireText(mobileEight, "mobileEight");
        if (StringUtils.hasText(payload.getMobileEight()) && !payload.getMobileEight().trim().equals(mobileEight)) {
            throw new BizException(ResultCode.PARAM_ERROR, "mobileEight与phone不一致");
        }
        requireValue(payload.getLoanAmount(), "loanAmount");
        requireText(payload.getCityName(), "cityName");
        requireValue(payload.getAge(), "age");
        requireValue(payload.getSex(), "sex");
        requireValue(payload.getHasHouse(), "hasHouse");
        requireValue(payload.getHasCar(), "hasCar");
        requireValue(payload.getHasCompany(), "hasCompany");
        requireValue(payload.getHasInsurance(), "hasInsurance");
        requireValue(payload.getHasSocial(), "hasSocial");
        requireValue(payload.getHasFund(), "hasFund");
        requireValue(payload.getZmfScore(), "zmfScore");
        requireValue(payload.getOverdue(), "overdue");
    }

    private void validatePreCheckPayload(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "payload is required");
        }
        requireText(payload.getPhoneMd5(), "phoneMd5");
        validateCommonPayloadFields(payload);
    }

    private void validateApplyPayload(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "payload is required");
        }
        requireText(payload.getPhone(), "phone");
        requireText(payload.getPhoneMd5(), "phoneMd5");
        validatePhoneMd5MatchesPhone(payload);
        requireText(payload.getName(), "name");
        validateCommonPayloadFields(payload);
    }

    private void validateCommonPayloadFields(CommonUpstreamPayloadDTO payload) {
        requireText(payload.getCity(), "city");
        requireText(payload.getIdCardPrefixFour(), "idCardPrefixFour");
        requireValue(payload.getAge(), "age");
        requireValue(payload.getGender(), "gender");
        requireValue(payload.getLoanTime(), "loanTime");
        requireValue(payload.getProfession(), "profession");
        requireValue(payload.getZhima(), "zhima");
        requireValue(payload.getProvidentFund(), "providentFund");
        requireValue(payload.getSocialSecurity(), "socialSecurity");
        requireValue(payload.getCommercialInsurance(), "commericalInsurance");
        requireValue(payload.getHouse(), "house");
        requireValue(payload.getOverdue(), "overdue");
        requireValue(payload.getVehicle(), "vehicle");
        requireValue(payload.getLoanAmount(), "loanAmount");
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ResultCode.PARAM_MISSING, fieldName);
        }
    }

    private void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw new BizException(ResultCode.PARAM_MISSING, fieldName);
        }
    }

    private void validatePhoneMd5MatchesPhone(CommonUpstreamPayloadDTO payload) {
        String md5FromPhone = CommonUpstreamMappingUtil.resolvePhoneMd5(null, payload.getPhone());
        String submittedMd5 = CommonUpstreamMappingUtil.resolvePhoneMd5(payload.getPhoneMd5(), null);
        if (!java.util.Objects.equals(md5FromPhone, submittedMd5)) {
            throw new BizException(ResultCode.PARAM_ERROR, "phoneMd5与phone不一致");
        }
    }

    private void enrichExtraInfo(StandardApplyData applyData, CommonUpstreamPayloadDTO payload, String scene, String orgCode) {
        Map<String, Object> extraInfo = applyData.getExtraInfo() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(applyData.getExtraInfo());
        extraInfo.put("scene", scene);
        extraInfo.put("orgCode", orgCode);
        extraInfo.put("upstreamPayload", buildUpstreamPayloadSnapshot(payload));
        String collisionNo = resolveCollisionNo(payload);
        if (StringUtils.hasText(collisionNo)) {
            extraInfo.put("collisionNo", collisionNo);
        }
        applyData.setExtraInfo(extraInfo);
    }

    private void logParsedPayload(String operation, String scene, Channel channel,
                                  CommonUpstreamPayloadDTO payload, StandardApplyData applyData) {
        log.info("【{}】渠道：{}，场景：{}，解密解析数据：{}",
                operation,
                channel == null ? null : channel.getChannelCode(),
                scene,
                JSON.toJSONString(buildMaskedPayloadLog(payload)));
        log.info("【{}】渠道：{}，场景：{}，标准化进件数据：{}",
                operation,
                channel == null ? null : channel.getChannelCode(),
                scene,
                JSON.toJSONString(buildStandardDataLog(applyData)));
    }

    private Map<String, Object> buildMaskedPayloadLog(CommonUpstreamPayloadDTO payload) {
        Map<String, Object> snapshot = new LinkedHashMap<>(buildUpstreamPayloadSnapshot(payload));
        putIfHasText(snapshot, "phone", maskPhone(payload == null ? null : payload.getPhone()));
        return snapshot;
    }

    private Map<String, Object> buildStandardDataLog(StandardApplyData data) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (data == null) {
            return snapshot;
        }
        snapshot.put("channelCode", data.getChannelCode());
        snapshot.put("phoneMd5", data.getPhoneMd5());
        putIfHasText(snapshot, "phone", maskPhone(data.getPhone()));
        snapshot.put("city", data.getWorkCity());
        snapshot.put("cityCode", data.getCityCode());
        snapshot.put("idCardPrefixFour", data.getIdCardPrefixFour());
        snapshot.put("age", data.getAge());
        snapshot.put("gender", data.getGender());
        snapshot.put("loanTime", data.getLoanTime());
        snapshot.put("profession", data.getProfession());
        snapshot.put("zhima", data.getZhima());
        snapshot.put("providentFund", data.getProvidentFund());
        snapshot.put("socialSecurity", data.getSocialSecurity());
        snapshot.put("commericalInsurance", data.getCommercialInsurance());
        snapshot.put("house", data.getHouse());
        snapshot.put("overdue", data.getOverdue());
        snapshot.put("vehicle", data.getVehicle());
        snapshot.put("loanAmount", data.getLoanAmount());
        return snapshot;
    }

    private Map<String, Object> buildUpstreamPayloadSnapshot(CommonUpstreamPayloadDTO payload) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (payload == null) {
            return snapshot;
        }
        snapshot.put("phoneMd5", resolvePayloadPhoneMd5(payload));
        snapshot.put("city", trimToNull(payload.getCity()));
        putIfHasText(snapshot, "cityCode", trimToNull(payload.getCityCode()));
        snapshot.put("idCardPrefixFour", trimToNull(payload.getIdCardPrefixFour()));
        snapshot.put("age", payload.getAge());
        snapshot.put("gender", payload.getGender());
        snapshot.put("loanTime", payload.getLoanTime());
        snapshot.put("profession", payload.getProfession());
        snapshot.put("zhima", payload.getZhima());
        snapshot.put("providentFund", payload.getProvidentFund());
        snapshot.put("socialSecurity", payload.getSocialSecurity());
        snapshot.put("commericalInsurance", payload.getCommercialInsurance());
        snapshot.put("house", payload.getHouse());
        snapshot.put("overdue", payload.getOverdue());
        snapshot.put("vehicle", payload.getVehicle());
        snapshot.put("loanAmount", payload.getLoanAmount());
        return snapshot;
    }

    private void validateApplyAgainstMatchedOrder(CollisionRecord matchedOrder, CommonUpstreamPayloadDTO payload,
                                                  StandardApplyData applyData) {
        Map<String, Object> storedSnapshot = parseStoredUpstreamPayload(matchedOrder);
        if (!storedSnapshot.isEmpty()) {
            compareField("phoneMd5", storedSnapshot.get("phoneMd5"), resolvePayloadPhoneMd5(payload));
            compareField("city", storedSnapshot.get("city"), payload == null ? null : trimToNull(payload.getCity()));
            compareStoredField(storedSnapshot, "cityCode", payload == null ? null : trimToNull(payload.getCityCode()));
            compareField("idCardPrefixFour", storedSnapshot.get("idCardPrefixFour"), payload == null ? null : trimToNull(payload.getIdCardPrefixFour()));
            compareField("age", storedSnapshot.get("age"), payload == null ? null : payload.getAge());
            compareField("gender", storedSnapshot.get("gender"), payload == null ? null : payload.getGender());
            compareField("loanTime", storedSnapshot.get("loanTime"), payload == null ? null : payload.getLoanTime());
            compareField("profession", storedSnapshot.get("profession"), payload == null ? null : payload.getProfession());
            compareField("zhima", storedSnapshot.get("zhima"), payload == null ? null : payload.getZhima());
            compareField("providentFund", storedSnapshot.get("providentFund"), payload == null ? null : payload.getProvidentFund());
            compareField("socialSecurity", storedSnapshot.get("socialSecurity"), payload == null ? null : payload.getSocialSecurity());
            compareField("commericalInsurance", storedSnapshot.get("commericalInsurance"), payload == null ? null : payload.getCommercialInsurance());
            compareField("house", storedSnapshot.get("house"), payload == null ? null : payload.getHouse());
            compareField("overdue", storedSnapshot.get("overdue"), payload == null ? null : payload.getOverdue());
            compareField("vehicle", storedSnapshot.get("vehicle"), payload == null ? null : payload.getVehicle());
            compareField("loanAmount", storedSnapshot.get("loanAmount"), payload == null ? null : payload.getLoanAmount());
            return;
        }

        log.warn("[COMMON-UPSTREAM] matched collision missing upstream payload snapshot, fallback to normalized comparison, collisionNo={}",
                matchedOrder.getCollisionNo());
        compareField("phoneMd5", matchedOrder.getPhoneMd5(), applyData.getPhoneMd5());
        compareField("age", matchedOrder.getAge(), applyData.getAge());
        if (StringUtils.hasText(matchedOrder.getCityCode()) && StringUtils.hasText(applyData.getCityCode())) {
            compareField("cityCode", matchedOrder.getCityCode(), applyData.getCityCode());
        }
        compareField("city", matchedOrder.getWorkCity(), applyData.getWorkCity());
        compareField("gender", matchedOrder.getGender(), applyData.getGender());
        compareField("profession", matchedOrder.getProfession(), applyData.getProfession());
        compareField("zhima", matchedOrder.getZhima(), applyData.getZhima());
        compareField("house", matchedOrder.getHouse(), applyData.getHouse());
        compareField("vehicle", matchedOrder.getVehicle(), applyData.getVehicle());
        compareField("providentFund", matchedOrder.getProvidentFund(), applyData.getProvidentFund());
        compareField("socialSecurity", matchedOrder.getSocialSecurity(), applyData.getSocialSecurity());
        compareField("commericalInsurance", matchedOrder.getCommercialInsurance(), applyData.getCommercialInsurance());
        compareField("overdue", matchedOrder.getOverdue(), applyData.getOverdue());
        compareField("loanAmount", matchedOrder.getLoanAmount(), applyData.getLoanAmount());
        compareField("loanTime", matchedOrder.getLoanTime(), applyData.getLoanTime());
    }

    private Map<String, Object> parseStoredUpstreamPayload(CollisionRecord matchedOrder) {
        if (matchedOrder == null || !StringUtils.hasText(matchedOrder.getExtJson())) {
            return java.util.Collections.emptyMap();
        }
        try {
            Map<String, Object> extJson = JSON.parseObject(matchedOrder.getExtJson(), Map.class);
            if (extJson == null) {
                return java.util.Collections.emptyMap();
            }
            Object upstreamPayload = extJson.get("upstreamPayload");
            if (upstreamPayload instanceof Map<?, ?> upstreamMap) {
                Map<String, Object> result = new LinkedHashMap<>();
                upstreamMap.forEach((key, value) -> result.put(String.valueOf(key), value));
                return result;
            }
        } catch (Exception ex) {
            log.warn("[COMMON-UPSTREAM] parse upstream payload snapshot failed, collisionNo={}", matchedOrder.getCollisionNo(), ex);
        }
        return java.util.Collections.emptyMap();
    }

    private void compareField(String fieldName, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "撞库记录与进件参数不一致: " + fieldName);
        }
    }

    private void compareStoredField(Map<String, Object> storedSnapshot, String fieldName, Object actual) {
        if (storedSnapshot.containsKey(fieldName)) {
            compareField(fieldName, storedSnapshot.get(fieldName), actual);
        }
    }

    private String resolvePayloadPhoneMd5(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            return null;
        }
        return CommonUpstreamMappingUtil.resolvePhoneMd5(payload.getPhoneMd5(), payload.getPhone());
    }

    private String trimToNull(String value) {
        return CommonUpstreamMappingUtil.trimToNull(value);
    }

    private String firstText(String first, String second) {
        String firstText = trimToNull(first);
        return firstText == null ? trimToNull(second) : firstText;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        String trimmed = phone.trim();
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private Map<String, Object> buildPreCheckResponse(PreCheckResult winner) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (winner == null || !winner.isPass()) {
            response.put("success", false);
            response.put("message", winner == null ? "no matched product" : winner.getRejectReason());
            return response;
        }

        putIfHasText(response, "orderId", winner.getOrderId());
        putIfHasText(response, "requestId", winner.getUuid());
        putIfHasText(response, "localOrderNo", winner.getLocalOrderNo());
        putIfHasText(response, "collisionNo", winner.getLocalOrderNo());
        putIfHasText(response, "productName", winner.getProductName());
        putIfHasText(response, "productLogo", winner.getProductLogo());
        putIfHasText(response, "companyName", winner.getCompanyName());
        if (winner.getProductId() != null) {
            response.put("productId", winner.getProductId());
        }
        if (winner.getProtocolList() != null && !winner.getProtocolList().isEmpty()) {
            response.put("protocolList", winner.getProtocolList());
        }
        if (winner.getPrice() != null) {
            response.put("price", winner.getPrice().toPlainString());
        }
        return response;
    }

    private void putIfHasText(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }

    private String resolveCollisionNo(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            return null;
        }
        if (StringUtils.hasText(payload.getCollisionNo())) {
            return payload.getCollisionNo().trim();
        }
        if (StringUtils.hasText(payload.getLocalOrderNo())) {
            return payload.getLocalOrderNo().trim();
        }
        return null;
    }

    private Map<String, Object> buildApplyResponse(String scene, String orgCode, Long productId, PushResult pushResult) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scene", scene);
        response.put("orgCode", orgCode);
        response.put("productId", productId);
        response.put("success", pushResult != null && pushResult.isSuccess());
        response.put("instCode", pushResult == null ? null : pushResult.getInstCode());
        response.put("thirdOrderNo", pushResult == null ? null : pushResult.getThirdOrderNo());
        response.put("message", normalizeApplyMessage(pushResult == null ? null : pushResult.getMsg()));
        return response;
    }

    private String normalizeApplyMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "进件失败";
        }

        String trimmed = message.trim();
        if ("push failed".equalsIgnoreCase(trimmed)) {
            return "进件失败";
        }
        if ("no matched product".equalsIgnoreCase(trimmed)) {
            return "未匹配到产品";
        }
        if (trimmed.startsWith("downstream error:")) {
            String detail = trimmed.substring("downstream error:".length()).trim();
            if (!StringUtils.hasText(detail)) {
                return "下游机构处理失败";
            }
            if (containsChinese(detail)) {
                return detail;
            }
            return "下游机构处理失败: " + detail;
        }
        return trimmed;
    }

    private boolean containsChinese(String value) {
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(value.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    private StandardApplyData toMobileEightStandardData(String channelCode, CommonUpstreamMobileEightPayloadDTO payload, String clientIp) {
        String phone = trimToNull(payload.getPhone());
        Map<String, Object> extraInfo = new LinkedHashMap<>();
        extraInfo.put("scene", "mobileEight");
        extraInfo.put("requestId", trimToNull(payload.getRequestId()));
        extraInfo.put("mobileEight", resolveMobileEight(payload.getMobileEight(), phone));
        extraInfo.put("upstreamPayload", buildMobileEightPayloadSnapshot(payload));
        return StandardApplyData.builder()
                .channelCode(channelCode)
                .name(null)
                .phone(phone)
                .phoneMd5(CommonUpstreamMappingUtil.resolvePhoneMd5(null, phone))
                .age(payload.getAge())
                .cityCode(trimToNull(payload.getCityName()))
                .workCity(trimToNull(payload.getCityName()))
                .gender(normalizeSex(payload.getSex()))
                .profession(Integer.valueOf(1).equals(payload.getHasCompany()) ? 3 : 1)
                .zhima(normalizeZmfScore(payload.getZmfScore()))
                .house(normalizeYesNoAsset(payload.getHasHouse()))
                .vehicle(normalizeYesNoAsset(payload.getHasCar()))
                .providentFund(normalizeHasAsset(payload.getHasFund()))
                .socialSecurity(normalizeHasAsset(payload.getHasSocial()))
                .commercialInsurance(normalizeHasAsset(payload.getHasInsurance()))
                .overdue(normalizeMobileEightOverdue(payload.getOverdue()))
                .loanAmount(normalizeWanAmount(payload.getLoanAmount()))
                .ip(StringUtils.hasText(payload.getIp()) ? payload.getIp().trim() : clientIp)
                .extraInfo(extraInfo)
                .build();
    }

    private Map<String, Object> buildMobileEightPayloadSnapshot(CommonUpstreamMobileEightPayloadDTO payload) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("requestId", trimToNull(payload.getRequestId()));
        snapshot.put("mobileEight", resolveMobileEight(payload.getMobileEight(), payload.getPhone()));
        snapshot.put("phoneMd5", CommonUpstreamMappingUtil.resolvePhoneMd5(null, payload.getPhone()));
        snapshot.put("loanAmount", payload.getLoanAmount());
        snapshot.put("cityName", trimToNull(payload.getCityName()));
        snapshot.put("age", payload.getAge());
        snapshot.put("sex", payload.getSex());
        snapshot.put("hasHouse", payload.getHasHouse());
        snapshot.put("hasCar", payload.getHasCar());
        snapshot.put("hasCompany", payload.getHasCompany());
        snapshot.put("hasInsurance", payload.getHasInsurance());
        snapshot.put("hasSocial", payload.getHasSocial());
        snapshot.put("hasFund", payload.getHasFund());
        snapshot.put("zmfScore", payload.getZmfScore());
        snapshot.put("overdue", payload.getOverdue());
        return snapshot;
    }

    private Map<String, Object> buildMobileEightPreCheckResponse(PreCheckResult winner) {
        Map<String, Object> response = buildPreCheckResponse(winner);
        if (winner != null && winner.getMatchSize() != null) {
            response.put("matchSize", winner.getMatchSize());
        }
        if (winner != null && winner.getMobileList() != null) {
            response.put("mobileList", winner.getMobileList());
        }
        return response;
    }

    private String resolveMobileEight(String mobileEight, String phone) {
        String submitted = trimToNull(mobileEight);
        String submittedPhone = trimToNull(phone);
        if (submittedPhone == null) {
            return submitted;
        }
        String derived = submittedPhone.length() <= 8
                ? submittedPhone
                : submittedPhone.substring(submittedPhone.length() - 8);
        return submitted == null ? derived : derived;
    }

    private Integer normalizeSex(Integer value) {
        return value == null || value != 1 && value != 2 ? 0 : value;
    }

    private Integer normalizeZmfScore(Integer value) {
        if (value == null || value == 0) {
            return null;
        }
        return switch (value) {
            case 1 -> 580;
            case 2 -> 680;
            case 3 -> 720;
            case 4 -> 620;
            default -> value;
        };
    }

    private Integer normalizeYesNoAsset(Integer value) {
        return Integer.valueOf(1).equals(value) ? 1 : 2;
    }

    private Integer normalizeHasAsset(Integer value) {
        return Integer.valueOf(1).equals(value) ? 1 : 0;
    }

    private Integer normalizeMobileEightOverdue(Integer value) {
        if (Integer.valueOf(3).equals(value)) {
            return 2;
        }
        return 1;
    }

    private Integer normalizeWanAmount(Integer value) {
        if (value == null) {
            return null;
        }
        return value > 1000 ? value : value * 10000;
    }

}
