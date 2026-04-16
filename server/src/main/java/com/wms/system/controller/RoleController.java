package com.wms.system.controller;

import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.RoleDTO;
import com.wms.system.service.RoleService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public Result<Map<String, Object>> listRoles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = roleService.listRoles(pageDTO, keyword, type, status);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getRole(@PathVariable Long id) {
        return Result.success(roleService.getRoleById(id));
    }

    @PostMapping
    public Result<Map<String, Object>> createRole(@Valid @RequestBody RoleDTO dto) {
        Long id = roleService.createRole(dto);
        return Result.success("角色创建成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        roleService.updateRole(id, dto);
        return Result.success("角色信息已更新", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success("角色已删除", null);
    }

    @PatchMapping("/{id}/enable")
    public Result<Void> enableRole(@PathVariable Long id) {
        roleService.enableRole(id);
        return Result.success("角色已启用", null);
    }

    @PatchMapping("/{id}/disable")
    public Result<Void> disableRole(@PathVariable Long id) {
        roleService.disableRole(id);
        return Result.success("角色已停用", null);
    }

    @GetMapping("/permissions/tree")
    public Result<List<Map<String, Object>>> getPermissionTree() {
        return Result.success(roleService.getPermissionTree());
    }

    @PatchMapping("/batch-enable")
    public Result<Void> batchEnable(@RequestBody Map<String, List<Long>> request) {
        roleService.batchEnable(request.get("ids"));
        return Result.success("已成功启用" + request.get("ids").size() + "个角色", null);
    }

    @PatchMapping("/batch-disable")
    public Result<Void> batchDisable(@RequestBody Map<String, List<Long>> request) {
        roleService.batchDisable(request.get("ids"));
        return Result.success("已成功停用" + request.get("ids").size() + "个角色", null);
    }

    @DeleteMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> request) {
        roleService.batchDelete(request.get("ids"));
        return Result.success("已成功删除" + request.get("ids").size() + "个角色", null);
    }
}
