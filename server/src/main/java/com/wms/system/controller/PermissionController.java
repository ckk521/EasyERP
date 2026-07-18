package com.wms.system.controller;

import com.wms.system.common.Result;
import com.wms.system.constant.PermissionConstants;
import com.wms.system.entity.SysPermission;
import com.wms.system.service.PermissionService;
import com.wms.system.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 权限管理Controller
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 获取当前用户的菜单权限列表
     */
    @GetMapping("/permissions/menus")
    public Result<List<String>> getUserMenuPermissions() {
        Long userId = UserContext.get().getUserId();
        List<String> menus = permissionService.getUserMenuPermissions(userId);
        return Result.success(menus);
    }

    /**
     * 获取当前用户的所有权限列表
     */
    @GetMapping("/permissions/user")
    public Result<Set<String>> getUserPermissions() {
        Long userId = UserContext.get().getUserId();
        Set<String> permissions = permissionService.getUserPermissionCodes(userId);
        return Result.success(permissions);
    }

    /**
     * 获取所有权限列表
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('" + PermissionConstants.SYSTEM_ROLE_VIEW + "')")
    public Result<List<SysPermission>> getAllPermissions() {
        List<SysPermission> permissions = permissionService.getAllPermissions();
        return Result.success(permissions);
    }
}