package com.wms.system.controller;

import com.wms.system.common.Result;
import com.wms.system.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/base/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }

    @GetMapping
    public Result<List<Map<String, Object>>> getCategories(@RequestParam(required = false) Long parentId) {
        return Result.success(categoryService.getCategoriesByParentId(parentId));
    }
}
