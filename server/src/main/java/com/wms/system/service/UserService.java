package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.UserDTO;
import com.wms.system.dto.UserUpdateDTO;
import com.wms.system.entity.SysRole;
import com.wms.system.entity.SysUser;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.exception.BusinessException;
import com.wms.system.repository.SysRoleRepository;
import com.wms.system.repository.SysUserRepository;
import com.wms.system.repository.SysWarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysWarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> listUsers(PageDTO pageDTO, String keyword, Long roleId, Long warehouseId, Integer status) {
        Page<SysUser> page = new Page<>(pageDTO.getPage(), pageDTO.getLimit());

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getName, keyword));
        }
        if (roleId != null) {
            wrapper.eq(SysUser::getRoleId, roleId);
        }
        if (warehouseId != null) {
            wrapper.eq(SysUser::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> pageResult = userRepository.selectPage(page, wrapper);

        List<SysUser> users = pageResult.getRecords();
        List<Long> roleIds = users.stream().map(SysUser::getRoleId).distinct().collect(java.util.stream.Collectors.toList());
        List<Long> warehouseIds = users.stream().map(SysUser::getWarehouseId).distinct().collect(java.util.stream.Collectors.toList());

        Map<Long, SysRole> roleMap = new HashMap<>();
        Map<Long, SysWarehouse> warehouseMap = new HashMap<>();

        if (!roleIds.isEmpty()) {
            roleRepository.selectBatchIds(roleIds).forEach(r -> roleMap.put(r.getId(), r));
        }
        if (!warehouseIds.isEmpty()) {
            warehouseRepository.selectBatchIds(warehouseIds).forEach(w -> warehouseMap.put(w.getId(), w));
        }

        List<Map<String, Object>> list = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("name", user.getName());
            map.put("phone", user.getPhone());
            map.put("email", user.getEmail());
            map.put("roleId", user.getRoleId());
            map.put("roleName", roleMap.get(user.getRoleId()) != null ? roleMap.get(user.getRoleId()).getName() : "-");
            map.put("warehouseId", user.getWarehouseId());
            map.put("warehouseName", user.getWarehouseId() != null && warehouseMap.get(user.getWarehouseId()) != null
                    ? warehouseMap.get(user.getWarehouseId()).getName() : "-");
            map.put("status", user.getStatus());
            map.put("statusText", user.getStatus() != null && user.getStatus() == 1 ? "正常" : "禁用");
            map.put("lastLoginTime", user.getLastLoginTime());
            map.put("createTime", user.getCreateTime());
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

    public Map<String, Object> getUserById(Long id) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        Map<String, Object> map = toMap(user);
        return map;
    }

    @Transactional
    public Long createUser(UserDTO dto) {
        log.info("createUser called with dto: {}", dto);
        // 检查用户名唯一性
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, dto.getUsername());
        long count = userRepository.selectCount(wrapper);
        log.info("User count check for username {}: {}", dto.getUsername(), count);
        if (count > 0) {
            throw new BusinessException(1002, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleId(dto.getRoleId());
        user.setWarehouseId(dto.getWarehouseId());
        user.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        log.info("About to insert user: {}", user);
        int result = userRepository.insert(user);
        log.info("Insert result: {}, user id after insert: {}", result, user.getId());

        return user.getId();
    }

    @Transactional
    public void updateUser(Long id, UserUpdateDTO dto) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleId(dto.getRoleId());
        user.setWarehouseId(dto.getWarehouseId());
        user.setUpdateTime(LocalDateTime.now());

        // 仅当密码不为空时更新密码
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.updateById(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        // 检查是否有业务数据关联
        // TODO: 检查入库、出库等业务数据

        userRepository.deleteById(id);
    }

    @Transactional
    public void enableUser(Long id, Long currentUserId) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (id.equals(currentUserId)) {
            throw new BusinessException(1003, "不能停用当前登录账号");
        }

        user.setStatus(1);
        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);
    }

    @Transactional
    public void disableUser(Long id, Long currentUserId) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (id.equals(currentUserId)) {
            throw new BusinessException(1003, "不能停用当前登录账号");
        }

        // 检查是否是超级管理员
        SysRole role = roleRepository.selectById(user.getRoleId());
        if (role != null && role.getIsSystem() == 1 && "SUPER_ADMIN".equals(role.getCode())) {
            throw new BusinessException(1004, "不能停用超级管理员");
        }

        user.setStatus(0);
        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);
    }

    public void resetPassword(Long id) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(1006, "该用户未设置邮箱，无法发送重置邮件");
        }
        // TODO: 发送重置邮件
    }

    private Map<String, Object> toMap(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("name", user.getName());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("roleId", user.getRoleId());
        map.put("warehouseId", user.getWarehouseId());
        map.put("status", user.getStatus());
        map.put("statusText", user.getStatus() != null && user.getStatus() == 1 ? "正常" : "禁用");
        map.put("lastLoginTime", user.getLastLoginTime());
        map.put("lastLoginIp", user.getLastLoginIp());
        map.put("createTime", user.getCreateTime());

        if (user.getRoleId() != null) {
            SysRole role = roleRepository.selectById(user.getRoleId());
            if (role != null) {
                map.put("roleName", role.getName());
            }
        }
        if (user.getWarehouseId() != null) {
            SysWarehouse warehouse = warehouseRepository.selectById(user.getWarehouseId());
            if (warehouse != null) {
                map.put("warehouseName", warehouse.getName());
            }
        }
        return map;
    }
}
