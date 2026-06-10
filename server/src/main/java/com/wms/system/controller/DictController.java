package com.wms.system.controller;

import com.wms.system.common.Result;
import com.wms.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /**
     * 查询指定类型的字典列表
     * @param type 字典类型 (department, position, work_status, skill_level, shift_type)
     */
    @GetMapping("/{type}")
    public Result<List<Map<String, Object>>> getDictByType(@PathVariable String type) {
        return Result.success(dictService.getDictByType(type));
    }

    /**
     * 查询所有字典类型
     */
    @GetMapping("/types")
    public Result<List<String>> getDictTypes() {
        return Result.success(dictService.getDictTypes());
    }

    /**
     * 查询所有字典（按类型分组）
     */
    @GetMapping("/all")
    public Result<Map<String, List<Map<String, Object>>>> getAllDicts() {
        return Result.success(dictService.getAllDicts());
    }
}