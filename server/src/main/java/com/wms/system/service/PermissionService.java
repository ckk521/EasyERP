package com.wms.system.service;

import com.wms.system.entity.SysPermission;
import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 根据用户ID获取用户所有权限编码
     * @param userId 用户ID
     * @return 权限编码集合
     */
    Set<String> getUserPermissionCodes(Long userId);

    /**
     * 根据用户ID获取用户菜单权限编码
     * @param userId 用户ID
     * @return 菜单权限编码列表
     */
    List<String> getUserMenuPermissions(Long userId);

    /**
     * 根据角色ID获取权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<SysPermission> getPermissionsByRoleId(Long roleId);

    /**
     * 获取所有权限列表
     * @return 权限列表
     */
    List<SysPermission> getAllPermissions();

    /**
     * 清除用户权限缓存
     * @param userId 用户ID
     */
    void clearUserPermissionCache(Long userId);
}