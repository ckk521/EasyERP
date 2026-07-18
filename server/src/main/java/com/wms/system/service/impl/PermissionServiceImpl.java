package com.wms.system.service.impl;

import com.wms.system.entity.SysPermission;
import com.wms.system.repository.SysPermissionRepository;
import com.wms.system.repository.SysRolePermissionRepository;
import com.wms.system.repository.SysUserRoleRepository;
import com.wms.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 *
 * MVP阶段使用内存缓存，Phase 2可升级为Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysUserRoleRepository userRoleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysPermissionRepository permissionRepository;

    /** 内存权限缓存 */
    private static final ConcurrentHashMap<Long, Set<String>> PERMISSION_CACHE = new ConcurrentHashMap<>();

    /** 权限缓存过期时间（分钟） */
    private static final long CACHE_EXPIRE_MINUTES = 30;

    /** 缓存创建时间 */
    private static final ConcurrentHashMap<Long, Long> CACHE_EXPIRE_TIME = new ConcurrentHashMap<>();

    @Override
    public Set<String> getUserPermissionCodes(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        // 1. 先从缓存获取
        if (isCacheValid(userId)) {
            Set<String> cachedPermissions = PERMISSION_CACHE.get(userId);
            if (cachedPermissions != null && !cachedPermissions.isEmpty()) {
                log.debug("从缓存获取用户权限, userId={}", userId);
                return new HashSet<>(cachedPermissions);
            }
        }

        // 2. 从数据库查询
        // 2.1 查询用户所有角色ID
        List<Long> roleIds = userRoleRepository.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            log.warn("用户无角色, userId={}", userId);
            return Collections.emptySet();
        }

        // 2.2 查询所有角色的权限ID（去重）
        Set<Long> permissionIds = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Long> permIds = rolePermissionRepository.selectPermissionIdsByRoleId(roleId);
            permissionIds.addAll(permIds);
        }

        if (permissionIds.isEmpty()) {
            log.warn("用户角色无权限, userId={}, roleIds={}", userId, roleIds);
            return Collections.emptySet();
        }

        // 2.3 查询权限编码
        List<SysPermission> permissions = permissionRepository.selectBatchIds(permissionIds);
        Set<String> permissionCodes = permissions.stream()
                .map(SysPermission::getCode)
                .collect(Collectors.toSet());

        // 3. 写入缓存
        if (!permissionCodes.isEmpty()) {
            PERMISSION_CACHE.put(userId, new HashSet<>(permissionCodes));
            CACHE_EXPIRE_TIME.put(userId, System.currentTimeMillis() + CACHE_EXPIRE_MINUTES * 60 * 1000);
            log.debug("用户权限已缓存, userId={}, count={}", userId, permissionCodes.size());
        }

        return permissionCodes;
    }

    @Override
    public List<String> getUserMenuPermissions(Long userId) {
        Set<String> allPermissions = getUserPermissionCodes(userId);
        return allPermissions.stream()
                .filter(code -> code.endsWith(":menu"))
                .collect(Collectors.toList());
    }

    @Override
    public List<SysPermission> getPermissionsByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }

        List<Long> permissionIds = rolePermissionRepository.selectPermissionIdsByRoleId(roleId);
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }

        return permissionRepository.selectBatchIds(permissionIds);
    }

    @Override
    public List<SysPermission> getAllPermissions() {
        return permissionRepository.selectList(null);
    }

    @Override
    public void clearUserPermissionCache(Long userId) {
        if (userId == null) {
            return;
        }
        PERMISSION_CACHE.remove(userId);
        CACHE_EXPIRE_TIME.remove(userId);
        log.info("已清除用户权限缓存, userId={}", userId);
    }

    /**
     * 清除所有权限缓存（测试用）
     */
    public void clearAllCache() {
        PERMISSION_CACHE.clear();
        CACHE_EXPIRE_TIME.clear();
        log.info("已清除所有权限缓存");
    }

    /**
     * 检查缓存是否有效（未过期）
     */
    private boolean isCacheValid(Long userId) {
        Long expireTime = CACHE_EXPIRE_TIME.get(userId);
        return expireTime != null && System.currentTimeMillis() < expireTime;
    }
}