package com.wms.system.controller;

import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.CustomerDTO;
import com.wms.system.dto.PageDTO;
import com.wms.system.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/base/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public Result<Map<String, Object>> listCustomers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = customerService.listCustomers(pageDTO, keyword, type, status);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getCustomer(@PathVariable Long id) {
        return Result.success(customerService.getCustomerById(id));
    }

    @PostMapping
    @OperationLog(module = "客户管理", action = "CREATE", description = "创建客户")
    public Result<Map<String, Object>> createCustomer(@RequestBody CustomerDTO dto) {
        Long id = customerService.createCustomer(dto);
        return Result.success("客户创建成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "客户管理", action = "UPDATE", description = "更新客户")
    public Result<Void> updateCustomer(@PathVariable Long id, @RequestBody CustomerDTO dto) {
        customerService.updateCustomer(id, dto);
        return Result.success("客户信息已更新", null);
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "客户管理", action = "DELETE", description = "删除客户")
    public Result<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return Result.success("客户已删除", null);
    }

    @PatchMapping("/{id}/enable")
    @OperationLog(module = "客户管理", action = "ENABLE", description = "启用客户")
    public Result<Void> enableCustomer(@PathVariable Long id) {
        customerService.enableCustomer(id);
        return Result.success("客户已启用", null);
    }

    @PatchMapping("/{id}/disable")
    @OperationLog(module = "客户管理", action = "DISABLE", description = "停用客户")
    public Result<Void> disableCustomer(@PathVariable Long id) {
        customerService.disableCustomer(id);
        return Result.success("客户已停用", null);
    }
}
