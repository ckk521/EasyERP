package com.wms.system.controller;

import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.SupplierProductDTO;
import com.wms.system.service.SupplierProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/base/supplier-products")
@RequiredArgsConstructor
public class SupplierProductController {

    private final SupplierProductService supplierProductService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        return Result.success(supplierProductService.list(supplierId, productId, pageDTO));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        return Result.success(supplierProductService.getById(id));
    }

    @PostMapping
    @OperationLog(module = "供应商商品管理", action = "CREATE", description = "创建供应商商品关系")
    public Result<Map<String, Object>> create(@RequestBody SupplierProductDTO dto) {
        Long id = supplierProductService.create(dto);
        return Result.success("关联成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "供应商商品管理", action = "UPDATE", description = "更新供应商商品关系")
    public Result<Void> update(@PathVariable Long id, @RequestBody SupplierProductDTO dto) {
        supplierProductService.update(id, dto);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "供应商商品管理", action = "DELETE", description = "删除供应商商品关系")
    public Result<Void> delete(@PathVariable Long id) {
        supplierProductService.delete(id);
        return Result.success("删除成功", null);
    }

    @PatchMapping("/{id}/enable")
    @OperationLog(module = "供应商商品管理", action = "ENABLE", description = "启用供应商商品关系")
    public Result<Void> enable(@PathVariable Long id) {
        supplierProductService.enable(id);
        return Result.success("已启用", null);
    }

    @PatchMapping("/{id}/disable")
    @OperationLog(module = "供应商商品管理", action = "DISABLE", description = "禁用供应商商品关系")
    public Result<Void> disable(@PathVariable Long id) {
        supplierProductService.disable(id);
        return Result.success("已禁用", null);
    }

    @GetMapping("/by-product/{productId}")
    public Result<List<Map<String, Object>>> getSuppliersByProduct(@PathVariable Long productId) {
        return Result.success(supplierProductService.getSuppliersByProduct(productId));
    }

    @GetMapping("/by-supplier/{supplierId}")
    public Result<List<Map<String, Object>>> getProductsBySupplier(@PathVariable Long supplierId) {
        return Result.success(supplierProductService.getProductsBySupplier(supplierId));
    }
}
