package com.wms.system.controller;

import com.wms.system.annotation.OperationLog;
import com.wms.system.common.Result;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.UserDTO;
import com.wms.system.dto.UserUpdateDTO;
import com.wms.system.service.UserService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer status) {

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPage(page);
        pageDTO.setLimit(limit);

        Map<String, Object> data = userService.listUsers(pageDTO, keyword, roleId, warehouseId, status);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getUser(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PostMapping
    @OperationLog(module = "用户管理", action = "CREATE", description = "创建用户")
    public Result<Map<String, Object>> createUser(@Valid @RequestBody UserDTO dto) {
        Long id = userService.createUser(dto);
        return Result.success("用户创建成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "用户管理", action = "UPDATE", description = "更新用户")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.success("用户信息已更新", null);
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "用户管理", action = "DELETE", description = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("用户已删除", null);
    }

    @PatchMapping("/{id}/enable")
    @OperationLog(module = "用户管理", action = "ENABLE", description = "启用用户")
    public Result<Void> enableUser(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = 1L; // TODO: 从认证信息获取
        userService.enableUser(id, currentUserId);
        return Result.success("用户已启用", null);
    }

    @PatchMapping("/{id}/disable")
    @OperationLog(module = "用户管理", action = "DISABLE", description = "禁用用户")
    public Result<Void> disableUser(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = 1L;
        userService.disableUser(id, currentUserId);
        return Result.success("用户已禁用", null);
    }

    @PostMapping("/{id}/reset-password")
    @OperationLog(module = "用户管理", action = "RESET_PASSWORD", description = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success("重置密码链接已发送到邮箱", null);
    }
}
