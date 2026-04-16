package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.entity.SysLoginLog;
import com.wms.system.entity.SysOperationLog;
import com.wms.system.repository.SysLoginLogRepository;
import com.wms.system.repository.SysOperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogService {

    private final SysOperationLogRepository operationLogRepository;
    private final SysLoginLogRepository loginLogRepository;

    public Map<String, Object> listOperationLogs(PageDTO pageDTO, LocalDateTime startTime, LocalDateTime endTime,
                                                   Long userId, String module, String action, String result) {
        Page<SysOperationLog> page = new Page<>(pageDTO.getPage(), pageDTO.getLimit());

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(SysOperationLog::getTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperationLog::getTime, endTime);
        }
        if (userId != null) {
            wrapper.eq(SysOperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysOperationLog::getModule, module);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(SysOperationLog::getAction, action);
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(SysOperationLog::getResult, result);
        }
        wrapper.orderByDesc(SysOperationLog::getTime);

        IPage<SysOperationLog> pageResult = operationLogRepository.selectPage(page, wrapper);

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("time", log.getTime());
            map.put("userId", log.getUserId());
            map.put("username", log.getUsername());
            map.put("module", log.getModule());
            map.put("action", log.getAction());
            map.put("object", log.getObject());
            map.put("ip", log.getIp());
            map.put("result", log.getResult());
            map.put("details", log.getDetails());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", list);
        resultMap.put("pagination", Map.of(
                "page", pageDTO.getPage(),
                "limit", pageDTO.getLimit(),
                "total", pageResult.getTotal(),
                "totalPages", pageResult.getPages()
        ));
        return resultMap;
    }

    public Map<String, Object> listLoginLogs(PageDTO pageDTO, LocalDateTime startTime, LocalDateTime endTime,
                                              Long userId, String result) {
        Page<SysLoginLog> page = new Page<>(pageDTO.getPage(), pageDTO.getLimit());

        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(SysLoginLog::getTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysLoginLog::getTime, endTime);
        }
        if (userId != null) {
            wrapper.eq(SysLoginLog::getUserId, userId);
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(SysLoginLog::getResult, result);
        }
        wrapper.orderByDesc(SysLoginLog::getTime);

        IPage<SysLoginLog> pageResult = loginLogRepository.selectPage(page, wrapper);

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("time", log.getTime());
            map.put("userId", log.getUserId());
            map.put("username", log.getUsername());
            map.put("action", log.getAction());
            map.put("ip", log.getIp());
            map.put("device", log.getDevice());
            map.put("browser", log.getBrowser());
            map.put("result", log.getResult());
            map.put("failReason", log.getFailReason());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", list);
        resultMap.put("pagination", Map.of(
                "page", pageDTO.getPage(),
                "limit", pageDTO.getLimit(),
                "total", pageResult.getTotal(),
                "totalPages", pageResult.getPages()
        ));
        return resultMap;
    }

    public Map<String, Object> getOperationLogById(Long id) {
        SysOperationLog log = operationLogRepository.selectById(id);
        if (log == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("time", log.getTime());
        map.put("userId", log.getUserId());
        map.put("username", log.getUsername());
        map.put("module", log.getModule());
        map.put("action", log.getAction());
        map.put("object", log.getObject());
        map.put("ip", log.getIp());
        map.put("result", log.getResult());
        map.put("details", log.getDetails());
        return map;
    }

    public Map<String, Object> getLoginLogById(Long id) {
        SysLoginLog log = loginLogRepository.selectById(id);
        if (log == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("time", log.getTime());
        map.put("userId", log.getUserId());
        map.put("username", log.getUsername());
        map.put("action", log.getAction());
        map.put("ip", log.getIp());
        map.put("device", log.getDevice());
        map.put("browser", log.getBrowser());
        map.put("result", log.getResult());
        map.put("failReason", log.getFailReason());
        return map;
    }
}
