package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.system.dto.ConfigDTO;
import com.wms.system.entity.SysConfig;
import com.wms.system.entity.SysConfigHistory;
import com.wms.system.exception.BusinessException;
import com.wms.system.repository.SysConfigHistoryRepository;
import com.wms.system.repository.SysConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigRepository configRepository;
    private final SysConfigHistoryRepository configHistoryRepository;

    public Map<String, Object> listConfigs(String category, String keyword) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(SysConfig::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysConfig::getCode, keyword).or().like(SysConfig::getName, keyword));
        }
        wrapper.orderByAsc(SysConfig::getSortOrder);

        List<SysConfig> configs = configRepository.selectList(wrapper);

        // 按分类分组
        Map<String, List<Map<String, Object>>> grouped = configs.stream()
                .map(this::toMap)
                .collect(Collectors.groupingBy(m -> (String) m.get("category")));

        Map<String, Object> result = new HashMap<>();
        grouped.forEach(result::put);

        return result;
    }

    public Map<String, Object> getConfigByCode(String code) {
        SysConfig config = findByCode(code);
        return toMap(config);
    }

    @Transactional
    public void updateConfig(String code, String value, String reason, Long operatorId, String operatorName) {
        SysConfig config = findByCode(code);

        // 保存历史
        SysConfigHistory history = new SysConfigHistory();
        history.setConfigId(config.getId());
        history.setOldValue(config.getValue());
        history.setNewValue(value);
        history.setOperatorId(operatorId);
        history.setOperatorName(operatorName);
        history.setReason(reason);
        history.setCreatedAt(LocalDateTime.now());
        configHistoryRepository.insert(history);

        // 更新配置
        config.setValue(value);
        config.setUpdateTime(LocalDateTime.now());
        configRepository.updateById(config);
    }

    @Transactional
    public void batchUpdateConfigs(List<ConfigDTO> configs, Long operatorId, String operatorName) {
        configs.forEach(dto -> {
            // 如果配置不存在，自动创建
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfig::getCode, dto.getCode());
            SysConfig existing = configRepository.selectOne(wrapper);

            if (existing == null) {
                SysConfig newConfig = new SysConfig();
                newConfig.setCode(dto.getCode());
                newConfig.setName(dto.getCode());
                newConfig.setValue(dto.getValue());
                newConfig.setType("string");
                newConfig.setCategory("未分类");
                newConfig.setDefaultValue(dto.getValue());
                newConfig.setDescription("");
                newConfig.setIsSystem(0);
                newConfig.setCreateTime(LocalDateTime.now());
                newConfig.setUpdateTime(LocalDateTime.now());
                configRepository.insert(newConfig);
            } else {
                updateConfig(dto.getCode(), dto.getValue(), dto.getReason(), operatorId, operatorName);
            }
        });
    }

    public Map<String, Object> listConfigHistory(String code, int page, int limit) {
        SysConfig config = findByCode(code);

        LambdaQueryWrapper<SysConfigHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfigHistory::getConfigId, config.getId());
        wrapper.orderByDesc(SysConfigHistory::getCreatedAt);

        List<SysConfigHistory> histories = configHistoryRepository.selectList(wrapper);

        List<Map<String, Object>> list = histories.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("oldValue", h.getOldValue());
            map.put("newValue", h.getNewValue());
            map.put("operatorId", h.getOperatorId());
            map.put("operatorName", h.getOperatorName());
            map.put("reason", h.getReason());
            map.put("createdAt", h.getCreatedAt());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("pagination", Map.of(
                "page", page,
                "limit", limit,
                "total", histories.size(),
                "totalPages", 1
        ));
        return result;
    }

    @Transactional
    public void rollbackConfig(String code, Long historyId, String reason, Long operatorId, String operatorName) {
        SysConfigHistory history = configHistoryRepository.selectById(historyId);
        if (history == null) {
            throw new BusinessException(3001, "配置历史不存在");
        }

        // 回滚到历史值
        updateConfig(code, history.getOldValue(), reason, operatorId, operatorName);
    }

    @Transactional
    public void resetConfig(String code, Long operatorId, String operatorName) {
        SysConfig config = findByCode(code);
        updateConfig(code, config.getDefaultValue(), "恢复默认配置", operatorId, operatorName);
    }

    @Transactional
    public void batchResetConfigs(List<String> codes, Long operatorId, String operatorName) {
        codes.forEach(code -> resetConfig(code, operatorId, operatorName));
    }

    public String exportConfigs(String category) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(SysConfig::getCategory, category);
        }
        List<SysConfig> configs = configRepository.selectList(wrapper);

        List<Map<String, Object>> exportData = configs.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("code", c.getCode());
            map.put("name", c.getName());
            map.put("value", c.getValue());
            map.put("category", c.getCategory());
            map.put("type", c.getType());
            map.put("defaultValue", c.getDefaultValue());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return exportData.toString(); // 实际应返回JSON或Excel
    }

    @Transactional
    public int importConfigs(String jsonData, String strategy) {
        // TODO: 解析JSON并导入
        // strategy: merge覆盖 / ignore跳过
        return 0;
    }

    public void validateConfig(String code, String value) {
        SysConfig config = findByCode(code);

        switch (code) {
            case "code.inbound_prefix":
            case "code.outbound_prefix":
            case "code.stocktake_prefix":
                if (!value.matches("^[A-Z]{1,5}$")) {
                    throw new BusinessException(3005, "单号前缀只能为字母，长度不超过5个字符");
                }
                break;
            case "business.validity_warning_days":
            case "business.stagnation_days":
                int days = Integer.parseInt(value);
                if (days <= 0) {
                    throw new BusinessException(3007, "预警天数必须为正整数");
                }
                if (days > 365) {
                    throw new BusinessException(3008, "预警天数不能超过365天");
                }
                break;
        }

        // 检查冲突
        if ("code.inbound_prefix".equals(code)) {
            SysConfig outboundConfig = findByCode("code.outbound_prefix");
            if (value.equals(outboundConfig.getValue())) {
                throw new BusinessException(3009, "入库单号前缀不能与出库单号前缀相同");
            }
        }
    }

    private SysConfig findByCode(String code) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getCode, code);
        SysConfig config = configRepository.selectOne(wrapper);
        if (config == null) {
            throw new BusinessException(3001, "配置不存在");
        }
        return config;
    }

    private Map<String, Object> toMap(SysConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", config.getId());
        map.put("category", config.getCategory());
        map.put("code", config.getCode());
        map.put("name", config.getName());
        map.put("value", config.getValue());
        map.put("type", config.getType());
        map.put("defaultValue", config.getDefaultValue());
        map.put("options", config.getOptions());
        map.put("description", config.getDescription());
        map.put("isSystem", config.getIsSystem());
        map.put("updateTime", config.getUpdateTime());
        return map;
    }
}
