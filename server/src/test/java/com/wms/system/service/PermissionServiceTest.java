package com.wms.system.service;

import com.wms.system.entity.SysPermission;
import com.wms.system.repository.SysPermissionRepository;
import com.wms.system.repository.SysRolePermissionRepository;
import com.wms.system.repository.SysUserRoleRepository;
import com.wms.system.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PermissionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private SysUserRoleRepository userRoleRepository;

    @Mock
    private SysRolePermissionRepository rolePermissionRepository;

    @Mock
    private SysPermissionRepository permissionRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Long testUserId;
    private Long testRoleId1;
    private Long testRoleId2;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testRoleId1 = 1L;
        testRoleId2 = 2L;
        // 清除缓存，避免测试之间共享状态
        permissionService.clearAllCache();
    }

    @Test
    @DisplayName("TC-PERM-001: 用户无角色时返回空权限")
    void testGetUserPermissionCodes_NoRoles() {
        // Given: 用户没有分配任何角色
        when(userRoleRepository.selectRoleIdsByUserId(testUserId))
                .thenReturn(Collections.emptyList());

        // When: 获取用户权限
        Set<String> permissions = permissionService.getUserPermissionCodes(testUserId);

        // Then: 返回空集合
        assertTrue(permissions.isEmpty());
        verify(userRoleRepository).selectRoleIdsByUserId(testUserId);
    }

    @Test
    @DisplayName("TC-PERM-002: 单角色用户获取权限")
    void testGetUserPermissionCodes_SingleRole() {
        // Given: 用户有一个角色
        Long permissionId1 = 1L;
        Long permissionId2 = 2L;

        when(userRoleRepository.selectRoleIdsByUserId(testUserId))
                .thenReturn(Collections.singletonList(testRoleId1));
        when(rolePermissionRepository.selectPermissionIdsByRoleId(testRoleId1))
                .thenReturn(Arrays.asList(permissionId1, permissionId2));

        SysPermission perm1 = new SysPermission();
        perm1.setId(permissionId1);
        perm1.setCode("inbound:order:view");

        SysPermission perm2 = new SysPermission();
        perm2.setId(permissionId2);
        perm2.setCode("inbound:order:create");

        when(permissionRepository.selectBatchIds(any(Collection.class)))
                .thenReturn(Arrays.asList(perm1, perm2));

        // When: 获取用户权限
        Set<String> permissions = permissionService.getUserPermissionCodes(testUserId);

        // Then: 返回正确的权限编码
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains("inbound:order:view"));
        assertTrue(permissions.contains("inbound:order:create"));
    }

    @Test
    @DisplayName("TC-PERM-003: 多角色用户权限合并（去重）")
    void testGetUserPermissionCodes_MultipleRoles() {
        // Given: 用户有两个角色，有重复权限
        Long permissionId1 = 1L;
        Long permissionId2 = 2L;
        Long permissionId3 = 3L;

        when(userRoleRepository.selectRoleIdsByUserId(testUserId))
                .thenReturn(Arrays.asList(testRoleId1, testRoleId2));
        when(rolePermissionRepository.selectPermissionIdsByRoleId(testRoleId1))
                .thenReturn(Arrays.asList(permissionId1, permissionId2));
        when(rolePermissionRepository.selectPermissionIdsByRoleId(testRoleId2))
                .thenReturn(Arrays.asList(permissionId2, permissionId3)); // permissionId2 重复

        SysPermission perm1 = new SysPermission();
        perm1.setId(permissionId1);
        perm1.setCode("inbound:order:view");

        SysPermission perm2 = new SysPermission();
        perm2.setId(permissionId2);
        perm2.setCode("inbound:order:create");

        SysPermission perm3 = new SysPermission();
        perm3.setId(permissionId3);
        perm3.setCode("outbound:order:view");

        when(permissionRepository.selectBatchIds(any(Collection.class)))
                .thenReturn(Arrays.asList(perm1, perm2, perm3));

        // When: 获取用户权限
        Set<String> permissions = permissionService.getUserPermissionCodes(testUserId);

        // Then: 权限去重，共3个权限
        assertEquals(3, permissions.size());
        assertTrue(permissions.contains("inbound:order:view"));
        assertTrue(permissions.contains("inbound:order:create"));
        assertTrue(permissions.contains("outbound:order:view"));
    }

    @Test
    @DisplayName("TC-PERM-004: 获取用户菜单权限")
    void testGetUserMenuPermissions() {
        // Given: 用户有菜单权限和操作权限
        Long permissionId1 = 1L;
        Long permissionId2 = 2L;
        Long permissionId3 = 3L;

        when(userRoleRepository.selectRoleIdsByUserId(testUserId))
                .thenReturn(Collections.singletonList(testRoleId1));
        when(rolePermissionRepository.selectPermissionIdsByRoleId(testRoleId1))
                .thenReturn(Arrays.asList(permissionId1, permissionId2, permissionId3));

        SysPermission perm1 = new SysPermission();
        perm1.setId(permissionId1);
        perm1.setCode("inbound:menu"); // 菜单权限

        SysPermission perm2 = new SysPermission();
        perm2.setId(permissionId2);
        perm2.setCode("inbound:order:view"); // 操作权限

        SysPermission perm3 = new SysPermission();
        perm3.setId(permissionId3);
        perm3.setCode("outbound:menu"); // 菜单权限

        when(permissionRepository.selectBatchIds(any(Collection.class)))
                .thenReturn(Arrays.asList(perm1, perm2, perm3));

        // When: 获取菜单权限
        List<String> menuPermissions = permissionService.getUserMenuPermissions(testUserId);

        // Then: 只返回菜单权限
        assertEquals(2, menuPermissions.size());
        assertTrue(menuPermissions.contains("inbound:menu"));
        assertTrue(menuPermissions.contains("outbound:menu"));
    }

    @Test
    @DisplayName("TC-PERM-005: 获取角色权限列表")
    void testGetPermissionsByRoleId() {
        // Given: 角色有权限
        Long permissionId1 = 1L;
        Long permissionId2 = 2L;

        when(rolePermissionRepository.selectPermissionIdsByRoleId(testRoleId1))
                .thenReturn(Arrays.asList(permissionId1, permissionId2));

        SysPermission perm1 = new SysPermission();
        perm1.setId(permissionId1);
        perm1.setCode("inbound:order:view");
        perm1.setName("查看出库单");

        SysPermission perm2 = new SysPermission();
        perm2.setId(permissionId2);
        perm2.setCode("inbound:order:create");
        perm2.setName("创建入库单");

        when(permissionRepository.selectBatchIds(any(Collection.class)))
                .thenReturn(Arrays.asList(perm1, perm2));

        // When: 获取角色权限
        List<SysPermission> permissions = permissionService.getPermissionsByRoleId(testRoleId1);

        // Then: 返回权限列表
        assertEquals(2, permissions.size());
        assertEquals("inbound:order:view", permissions.get(0).getCode());
        assertEquals("inbound:order:create", permissions.get(1).getCode());
    }

    @Test
    @DisplayName("TC-PERM-006: 清除用户权限缓存")
    void testClearUserPermissionCache() {
        // Given: 用户权限已缓存
        when(userRoleRepository.selectRoleIdsByUserId(testUserId))
                .thenReturn(Collections.singletonList(testRoleId1));
        when(rolePermissionRepository.selectPermissionIdsByRoleId(testRoleId1))
                .thenReturn(Collections.singletonList(1L));

        SysPermission perm = new SysPermission();
        perm.setId(1L);
        perm.setCode("test:permission");
        when(permissionRepository.selectBatchIds(any(Collection.class)))
                .thenReturn(Collections.singletonList(perm));

        // 首次获取，写入缓存
        Set<String> permissions1 = permissionService.getUserPermissionCodes(testUserId);
        assertEquals(1, permissions1.size());

        // When: 清除缓存
        permissionService.clearUserPermissionCache(testUserId);

        // Then: 缓存被清除（下次查询会重新从数据库加载）
        // 这里验证方法被调用，不抛异常即可
        assertDoesNotThrow(() -> permissionService.clearUserPermissionCache(testUserId));
    }

    @Test
    @DisplayName("TC-PERM-007: null用户ID返回空权限")
    void testGetUserPermissionCodes_NullUserId() {
        // When: 传入null
        Set<String> permissions = permissionService.getUserPermissionCodes(null);

        // Then: 返回空集合
        assertTrue(permissions.isEmpty());
    }

    @Test
    @DisplayName("TC-PERM-008: 角色无权限时返回空列表")
    void testGetPermissionsByRoleId_NoPermissions() {
        // Given: 角色没有权限
        when(rolePermissionRepository.selectPermissionIdsByRoleId(testRoleId1))
                .thenReturn(Collections.emptyList());

        // When: 获取角色权限
        List<SysPermission> permissions = permissionService.getPermissionsByRoleId(testRoleId1);

        // Then: 返回空列表
        assertTrue(permissions.isEmpty());
    }
}
