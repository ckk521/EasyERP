package com.wms.system.controller;

import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.SupplierDTO;
import com.wms.system.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/base/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public Result<Map<String, Object>> listSuppliers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = supplierService.listSuppliers(pageDTO, keyword, status);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getSupplier(@PathVariable Long id) {
        return Result.success(supplierService.getSupplierById(id));
    }

    @PostMapping
    @OperationLog(module = "供应商管理", action = "CREATE", description = "创建供应商")
    public Result<Map<String, Object>> createSupplier(@RequestBody SupplierDTO dto) {
        Long id = supplierService.createSupplier(dto);
        return Result.success("供应商创建成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "供应商管理", action = "UPDATE", description = "更新供应商")
    public Result<Void> updateSupplier(@PathVariable Long id, @RequestBody SupplierDTO dto) {
        supplierService.updateSupplier(id, dto);
        return Result.success("供应商信息已更新", null);
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "供应商管理", action = "DELETE", description = "删除供应商")
    public Result<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return Result.success("供应商已删除", null);
    }

    @PatchMapping("/{id}/enable")
    @OperationLog(module = "供应商管理", action = "ENABLE", description = "启用供应商")
    public Result<Void> enableSupplier(@PathVariable Long id) {
        supplierService.enableSupplier(id);
        return Result.success("供应商已启用", null);
    }

    @PatchMapping("/{id}/disable")
    @OperationLog(module = "供应商管理", action = "DISABLE", description = "停用供应商")
    public Result<Void> disableSupplier(@PathVariable Long id) {
        supplierService.disableSupplier(id);
        return Result.success("供应商已停用", null);
    }
}
