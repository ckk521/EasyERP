package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.RoleDTO;
import com.wms.system.entity.SysPermission;
import com.wms.system.entity.SysRole;
import com.wms.system.entity.SysRolePermission;
import com.wms.system.entity.SysUser;
import com.wms.system.exception.BusinessException;
import com.wms.system.repository.SysPermissionRepository;
import com.wms.system.repository.SysRolePermissionRepository;
import com.wms.system.repository.SysRoleRepository;
import com.wms.system.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysUserRepository userRepository;

    public Map<String, Object> listRoles(PageDTO pageDTO, String keyword, Integer type, Integer status) {
        Page<SysRole> page = new Page<>(pageDTO.getPage(), pageDTO.getLimit());

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysRole::getCode, keyword).or().like(SysRole::getName, keyword));
        }
        if (type != null) {
            wrapper.eq(SysRole::getType, type);
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByDesc(SysRole::getCreateTime);

        IPage<SysRole> pageResult = roleRepository.selectPage(page, wrapper);
        List<SysRole> roles = pageResult.getRecords();

        // 统计每个角色的用户数和权限数
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(java.util.stream.Collectors.toList());
        Map<Long, Long> userCountMap = new HashMap<>();
        Map<Long, Long> permCountMap = new HashMap<>();

        if (!roleIds.isEmpty()) {
            // 统计用户数
            roleIds.forEach(roleId -> {
                LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
                userWrapper.eq(SysUser::getRoleId, roleId);
                userCountMap.put(roleId, userRepository.selectCount(userWrapper));
            });

            // 统计权限数
            roleIds.forEach(roleId -> {
                LambdaQueryWrapper<SysRolePermission> permWrapper = new LambdaQueryWrapper<>();
                permWrapper.eq(SysRolePermission::getRoleId, roleId);
                permCountMap.put(roleId, rolePermissionRepository.selectCount(permWrapper));
            });
        }

        List<Map<String, Object>> list = roles.stream().map(role -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", role.getId());
            map.put("code", role.getCode());
            map.put("name", role.getName());
            map.put("type", role.getType());
            map.put("typeText", role.getType() != null && role.getType() == 1 ? "预置" : "自定义");
            map.put("description", role.getDescription());
            map.put("userCount", userCountMap.getOrDefault(role.getId(), 0L));
            map.put("permissionCount", permCountMap.getOrDefault(role.getId(), 0L));
            map.put("status", role.getStatus());
            map.put("statusText", role.getStatus() != null && role.getStatus() == 1 ? "启用" : "停用");
            map.put("isSystem", role.getIsSystem());
            map.put("createTime", role.getCreateTime());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("pagination", Map.of(
                "page", pageDTO.getPage(),
                "limit", pageDTO.getLimit(),
                "total", pageResult.getTotal(),
                "totalPages", pageResult.getPages()
        ));
        return result;
    }

    public Map<String, Object> getRoleById(Long id) {
        SysRole role = roleRepository.selectById(id);
        if (role == null) {
            throw new BusinessException(2001, "角色不存在");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", role.getId());
        map.put("code", role.getCode());
        map.put("name", role.getName());
        map.put("type", role.getType());
        map.put("typeText", role.getType() != null && role.getType() == 1 ? "预置" : "自定义");
        map.put("description", role.getDescription());
        map.put("status", role.getStatus());
        map.put("isSystem", role.getIsSystem());
        map.put("createTime", role.getCreateTime());

        // 获取权限列表
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, id);
        List<Long> permIds = rolePermissionRepository.selectList(wrapper).stream()
                .map(SysRolePermission::getPermissionId)
                .collect(java.util.stream.Collectors.toList());

        List<SysPermission> permissions = permIds.isEmpty() ? Collections.emptyList()
                : permissionRepository.selectBatchIds(permIds);

        map.put("permissions", permissions.stream().map(p -> {
            Map<String, Object> pm = new HashMap<>();
            pm.put("id", p.getId());
            pm.put("code", p.getCode());
            pm.put("name", p.getName());
            pm.put("module", p.getModule());
            return pm;
        }).collect(java.util.stream.Collectors.toList()));

        return map;
    }

    @Transactional
    public Long createRole(RoleDTO dto) {
        // 检查编码唯一性
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCode, dto.getCode());
        if (roleRepository.selectCount(wrapper) > 0) {
            throw new BusinessException(2002, "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setType(2); // 自定义角色
        role.setDescription(dto.getDescription());
        role.setStatus(1);
        role.setIsSystem(0);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleRepository.insert(role);

        // 保存权限关联
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            saveRolePermissions(role.getId(), dto.getPermissionIds());
        }

        return role.getId();
    }

    @Transactional
    public void updateRole(Long id, RoleDTO dto) {
        SysRole role = roleRepository.selectById(id);
        if (role == null) {
            throw new BusinessException(2001, "角色不存在");
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new BusinessException(2003, "预置角色不可编辑");
        }

        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setUpdateTime(LocalDateTime.now());
        roleRepository.updateById(role);

        // 更新权限关联
        if (dto.getPermissionIds() != null) {
            // 删除旧权限
            LambdaQueryWrapper<SysRolePermission> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(SysRolePermission::getRoleId, id);
            rolePermissionRepository.delete(delWrapper);

            // 保存新权限
            saveRolePermissions(id, dto.getPermissionIds());
        }
    }

    @Transactional
    public void deleteRole(Long id) {
        SysRole role = roleRepository.selectById(id);
        if (role == null) {
            throw new BusinessException(2001, "角色不存在");
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new BusinessException(2004, "预置角色不可删除");
        }

        // 检查是否有用户关联
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getRoleId, id);
        long userCount = userRepository.selectCount(userWrapper);
        if (userCount > 0) {
            throw new BusinessException(2005, "该角色下有" + userCount + "个用户，无法删除");
        }

        // 删除权限关联
        LambdaQueryWrapper<SysRolePermission> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(SysRolePermission::getRoleId, id);
        rolePermissionRepository.delete(permWrapper);

        // 删除角色
        roleRepository.deleteById(id);
    }

    @Transactional
    public void enableRole(Long id) {
        SysRole role = roleRepository.selectById(id);
        if (role == null) {
            throw new BusinessException(2001, "角色不存在");
        }
        role.setStatus(1);
        role.setUpdateTime(LocalDateTime.now());
        roleRepository.updateById(role);
    }

    @Transactional
    public void disableRole(Long id) {
        SysRole role = roleRepository.selectById(id);
        if (role == null) {
            throw new BusinessException(2001, "角色不存在");
        }
        role.setStatus(0);
        role.setUpdateTime(LocalDateTime.now());
        roleRepository.updateById(role);
    }

    public List<Map<String, Object>> getPermissionTree() {
        List<SysPermission> allPermissions = permissionRepository.selectList(null);

        // 按模块分组
        Map<String, List<SysPermission>> byModule = allPermissions.stream()
                .collect(Collectors.groupingBy(p -> p.getModule() != null ? p.getModule() : "其他"));

        List<Map<String, Object>> tree = new ArrayList<>();
        byModule.forEach((moduleName, permissions) -> {
            Map<String, Object> moduleNode = new HashMap<>();
            moduleNode.put("id", moduleName.hashCode());
            moduleNode.put("code", moduleName.toLowerCase());
            moduleNode.put("name", moduleName);
            moduleNode.put("children", permissions.stream().map(p -> {
                Map<String, Object> permNode = new HashMap<>();
                permNode.put("id", p.getId());
                permNode.put("code", p.getCode());
                permNode.put("name", p.getName());
                return permNode;
            }).collect(java.util.stream.Collectors.toList()));
            tree.add(moduleNode);
        });

        return tree;
    }

    @Transactional
    public void batchEnable(List<Long> ids) {
        ids.forEach(this::enableRole);
    }

    @Transactional
    public void batchDisable(List<Long> ids) {
        ids.forEach(this::disableRole);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        ids.forEach(this::deleteRole);
    }

    private void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        for (Long permId : permissionIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rp.setCreateTime(LocalDateTime.now());
            rolePermissionRepository.insert(rp);
        }
    }
}
