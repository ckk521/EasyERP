package com.wms.system.controller;

import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import com.wms.system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping("/operations")
    public Result<Map<String, Object>> listOperationLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = logService.listOperationLogs(pageDTO, startTime, endTime, userId, module, action, result);
        return Result.success(data);
    }

    @GetMapping("/login")
    public Result<Map<String, Object>> listLoginLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String result) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = logService.listLoginLogs(pageDTO, startTime, endTime, userId, result);
        return Result.success(data);
    }

    @GetMapping("/operation/{id}")
    public Result<Map<String, Object>> getOperationLog(@PathVariable Long id) {
        return Result.success(logService.getOperationLogById(id));
    }

    @GetMapping("/login/{id}")
    public Result<Map<String, Object>> getLoginLog(@PathVariable Long id) {
        return Result.success(logService.getLoginLogById(id));
    }
}
