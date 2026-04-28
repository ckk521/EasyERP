package com.wms.system.controller;

import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.ProductDTO;
import com.wms.system.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/base/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Result<Map<String, Object>> listProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = productService.listProducts(pageDTO, keyword, categoryId, status);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getProduct(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }

    @PostMapping
    @OperationLog(module = "商品管理", action = "CREATE", description = "创建商品")
    public Result<Map<String, Object>> createProduct(@RequestBody ProductDTO dto) {
        Long id = productService.createProduct(dto);
        return Result.success("商品创建成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "商品管理", action = "UPDATE", description = "更新商品")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody ProductDTO dto) {
        productService.updateProduct(id, dto);
        return Result.success("商品信息已更新", null);
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "商品管理", action = "DELETE", description = "删除商品")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success("商品已删除", null);
    }

    @PatchMapping("/{id}/enable")
    @OperationLog(module = "商品管理", action = "ENABLE", description = "启用商品")
    public Result<Void> enableProduct(@PathVariable Long id) {
        productService.enableProduct(id);
        return Result.success("商品已启用", null);
    }

    @PatchMapping("/{id}/disable")
    @OperationLog(module = "商品管理", action = "DISABLE", description = "禁用商品")
    public Result<Void> disableProduct(@PathVariable Long id) {
        productService.disableProduct(id);
        return Result.success("商品已禁用", null);
    }
}
