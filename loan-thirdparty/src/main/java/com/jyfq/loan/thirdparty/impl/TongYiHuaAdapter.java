package com.jyfq.loan.thirdparty.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.thirdparty.AbstractInstitutionAdapter;
import com.jyfq.loan.thirdparty.model.PreCheckRequest;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushRequest;
import com.jyfq.loan.thirdparty.model.PushResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 【通易花】下游适配器实现 (AES/CBC)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TongYiHuaAdapter extends AbstractInstitutionAdapter {

    private final InstitutionMapper institutionMapper;

    @Override
    public String getInstCode() {
        return "tongyihua"; // 约定编码
    }

    @Override
    public PreCheckResult preCheck(PreCheckRequest req) {
        // 1. 获取机构配置 (由基类处理 URL, 此处需要 Key)
        Institution inst = institutionMapper.selectOne(new LambdaQueryWrapper<Institution>()
                .eq(Institution::getInstCode, getInstCode()));
        
        // 2. 组装下游请求体 (字段 Mapping)
        JSONObject data = new JSONObject();
        data.put("mobile", req.getPhone());
        data.put("idCard", req.getIdCard());
        data.put("name", req.getName());
        // ... 继续补充详细映射逻辑

        // 3. 发送请求 (doPost 内部会自动加密)
        JSONObject resp = doPost(inst.getApiPushUrl(), data, JSONObject.class);

        // 4. 解析结果
        if (resp != null && resp.getInteger("code") == 1) {
            JSONObject resultData = resp.getJSONObject("data");
            return PreCheckResult.builder()
                    .pass(true)
                    .instCode(getInstCode())
                    .price(resultData != null ? resultData.getBigDecimal("price") : BigDecimal.ZERO)
                    .uuid(resultData != null ? resultData.getString("uuid") : null)
                    .build();
        }

        return PreCheckResult.builder()
                .pass(false)
                .instCode(getInstCode())
                .rejectReason(resp != null ? resp.getString("msg") : "网络超时")
                .build();
    }

    @Override
    public PushResult push(PushRequest req) {
        log.info("[PUSH] 执行通易花正式推单: orderNo={}", req.getOrderNo());
        
        Institution inst = institutionMapper.selectOne(new LambdaQueryWrapper<Institution>()
                .eq(Institution::getInstCode, getInstCode()));

        // 1. 映射进件明细数据
        JSONObject data = new JSONObject();
        data.put("orderNo", req.getOrderNo());
        
        // 关键：从 StandardApplyData 转换为下游字段
        if (req.getStandardData() != null) {
            data.put("mobile", req.getStandardData().getPhone());
            data.put("name", req.getStandardData().getName());
            data.put("idCard", req.getStandardData().getIdCard());
            data.put("age", req.getStandardData().getAge());
            data.put("city", req.getStandardData().getCityCode());
            data.put("house", req.getStandardData().getHouse());
            data.put("car", req.getStandardData().getVehicle());
            data.put("sesameScore", req.getStandardData().getZhima());
            // ... 继续补充其它学历、婚姻等字段的具体映射
        }

        // 2. 调用正式进件接口
        try {
            JSONObject resp = doPost(inst.getApiPushUrl() + "/apply", data, JSONObject.class);
            
            if (resp != null && resp.getInteger("code") == 1) {
                return PushResult.success(resp.getString("msg"));
            }
            return PushResult.failure(resp != null ? resp.getString("msg") : "推送失败");
        } catch (Exception e) {
            log.error("[PUSH] 通易花推单异常: orderNo={}", req.getOrderNo(), e);
            return PushResult.failure("下游接口异常: " + e.getMessage());
        }
    }

    @Override
    protected String encrypt(String plainText) {
        Institution inst = institutionMapper.selectOne(new LambdaQueryWrapper<Institution>()
                .eq(Institution::getInstCode, getInstCode()));
        // 通易花要求 CBC, IV=Key
        return AesUtil.encryptCBC(plainText, inst.getAppKey(), inst.getAppKey());
    }

    @Override
    protected String decrypt(String cipherText) {
        Institution inst = institutionMapper.selectOne(new LambdaQueryWrapper<Institution>()
                .eq(Institution::getInstCode, getInstCode()));
        return AesUtil.decryptCBC(cipherText, inst.getAppKey(), inst.getAppKey());
    }
}
