package com.wms.system.controller;

import com.wms.system.common.Result;
import com.wms.system.dto.ConfigDTO;
import com.wms.system.service.ConfigService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public Result<Map<String, Object>> listConfigs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return Result.success(configService.listConfigs(category, keyword));
    }

    @GetMapping("/{code}")
    public Result<Map<String, Object>> getConfig(@PathVariable String code) {
        return Result.success(configService.getConfigByCode(code));
    }

    @PutMapping("/{code}")
    public Result<Void> updateConfig(
            @PathVariable String code,
            @RequestBody ConfigDTO dto,
            Authentication authentication) {
        Long operatorId = 1L; // TODO: 从认证信息获取
        String operatorName = "系统管理员";
        configService.updateConfig(code, dto.getValue(), dto.getReason(), operatorId, operatorName);
        return Result.success("配置已保存", null);
    }

    @PutMapping("/batch-update")
    public Result<Void> batchUpdateConfigs(
            @RequestBody List<ConfigDTO> configs,
            Authentication authentication) {
        Long operatorId = 1L;
        String operatorName = "系统管理员";
        configService.batchUpdateConfigs(configs, operatorId, operatorName);
        return Result.success("配置已保存", null);
    }

    @GetMapping("/{code}/history")
    public Result<Map<String, Object>> getConfigHistory(
            @PathVariable String code,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(configService.listConfigHistory(code, page, limit));
    }

    @PostMapping("/{code}/rollback")
    public Result<Void> rollbackConfig(
            @PathVariable String code,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long operatorId = 1L;
        String operatorName = "系统管理员";
        configService.rollbackConfig(code, (Long) request.get("historyId"),
                (String) request.get("reason"), operatorId, operatorName);
        return Result.success("配置已回滚", null);
    }

    @PostMapping("/{code}/reset")
    public Result<Void> resetConfig(
            @PathVariable String code,
            Authentication authentication) {
        Long operatorId = 1L;
        String operatorName = "系统管理员";
        configService.resetConfig(code, operatorId, operatorName);
        return Result.success("已恢复默认配置", null);
    }

    @PostMapping("/batch-reset")
    public Result<Void> batchResetConfigs(
            @RequestBody Map<String, List<String>> request,
            Authentication authentication) {
        Long operatorId = 1L;
        String operatorName = "系统管理员";
        configService.batchResetConfigs(request.get("codes"), operatorId, operatorName);
        return Result.success("已恢复" + request.get("codes").size() + "项默认配置", null);
    }

    @GetMapping("/export")
    public Result<String> exportConfigs(@RequestParam(required = false) String category) {
        return Result.success(configService.exportConfigs(category));
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importConfigs(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "strategy", defaultValue = "merge") String strategy) {
        // TODO: 处理文件导入
        return Result.success("配置导入成功", Map.of("count", 0));
    }
}
