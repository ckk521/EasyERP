package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.UserCreateDTO;
import com.wms.system.dto.UserDTO;
import com.wms.system.dto.UserUpdateDTO;
import com.wms.system.entity.SysDict;
import com.wms.system.entity.SysRole;
import com.wms.system.entity.SysUser;
import com.wms.system.entity.SysUserRole;
import com.wms.system.entity.SysUserWarehouse;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.exception.BusinessException;
import com.wms.system.repository.SysDictRepository;
import com.wms.system.repository.SysRoleRepository;
import com.wms.system.repository.SysUserRepository;
import com.wms.system.repository.SysUserRoleRepository;
import com.wms.system.repository.SysUserWarehouseRepository;
import com.wms.system.repository.SysWarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final SysWarehouseRepository warehouseRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysUserWarehouseRepository userWarehouseRepository;
    private final SysDictRepository dictRepository;
    private final PasswordEncoder passwordEncoder;

    // 字段长度常量
    private static final int MAX_EMPLOYEE_NO_LENGTH = 20;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_EMAIL_LENGTH = 100;

    // 工号格式校验：只允许字母、数字、下划线
    private static final Pattern EMPLOYEE_NO_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    // 手机号格式校验
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    // 邮箱格式校验
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Map<String, Object> listUsers(PageDTO pageDTO, String keyword, Long roleId, Long warehouseId, Integer status,
                                          Integer department, Integer workStatus) {
        Page<SysUser> page = new Page<>(pageDTO.getPage(), pageDTO.getLimit());

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getName, keyword)
                    .or().like(SysUser::getEmployeeNo, keyword));
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (department != null) {
            wrapper.eq(SysUser::getDepartment, department);
        }
        if (workStatus != null) {
            wrapper.eq(SysUser::getWorkStatus, workStatus);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> pageResult = userRepository.selectPage(page, wrapper);

        List<SysUser> users = pageResult.getRecords();
        List<Map<String, Object>> list = users.stream().map(this::toMap).collect(Collectors.toList());

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

        // 获取用户角色列表
        LambdaQueryWrapper<SysUserRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysUserRole::getUserId, id);
        List<SysUserRole> userRoles = userRoleRepository.selectList(roleWrapper);

        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            List<SysRole> roles = roleRepository.selectBatchIds(roleIds);
            List<Map<String, Object>> roleList = roles.stream().map(role -> {
                Map<String, Object> roleMap = new HashMap<>();
                roleMap.put("id", role.getId());
                roleMap.put("code", role.getCode());
                roleMap.put("name", role.getName());
                return roleMap;
            }).collect(Collectors.toList());
            map.put("roles", roleList);
        } else {
            map.put("roles", Collections.emptyList());
        }

        // 获取用户仓库列表
        LambdaQueryWrapper<SysUserWarehouse> warehouseWrapper = new LambdaQueryWrapper<>();
        warehouseWrapper.eq(SysUserWarehouse::getUserId, id);
        List<SysUserWarehouse> userWarehouses = userWarehouseRepository.selectList(warehouseWrapper);

        if (!userWarehouses.isEmpty()) {
            List<Long> warehouseIds = userWarehouses.stream().map(SysUserWarehouse::getWarehouseId).collect(Collectors.toList());
            List<SysWarehouse> warehouses = warehouseRepository.selectBatchIds(warehouseIds);
            List<Map<String, Object>> warehouseList = warehouses.stream().map(wh -> {
                Map<String, Object> whMap = new HashMap<>();
                whMap.put("id", wh.getId());
                whMap.put("code", wh.getCode());
                whMap.put("name", wh.getName());
                return whMap;
            }).collect(Collectors.toList());
            map.put("warehouses", warehouseList);
        } else {
            map.put("warehouses", Collections.emptyList());
        }

        return map;
    }

    @Transactional
    public Long createUser(UserCreateDTO dto) {
        log.info("createUser called with dto: {}", dto);

        // 自动生成员工工号（如果未提供）
        String employeeNo = dto.getEmployeeNo();
        if (!StringUtils.hasText(employeeNo)) {
            employeeNo = generateEmployeeNo();
        }

        // 校验必填字段
        validateRequiredFields(dto);

        // 校验字段长度
        if (employeeNo.length() > MAX_EMPLOYEE_NO_LENGTH) {
            throw new BusinessException(1012, "工号不能超过" + MAX_EMPLOYEE_NO_LENGTH + "个字符");
        }

        // 校验工号格式
        validateEmployeeNoFormat(employeeNo);

        // 校验手机号格式
        if (StringUtils.hasText(dto.getPhone())) {
            validatePhoneFormat(dto.getPhone());
        }

        // 校验邮箱格式
        if (StringUtils.hasText(dto.getEmail())) {
            validateEmailFormat(dto.getEmail());
        }

        // 检查工号唯一性
        LambdaQueryWrapper<SysUser> employeeNoWrapper = new LambdaQueryWrapper<>();
        employeeNoWrapper.eq(SysUser::getEmployeeNo, employeeNo);
        if (userRepository.selectCount(employeeNoWrapper) > 0) {
            throw new BusinessException(1002, "员工工号已存在，请使用其他工号");
        }

        // 检查用户名唯一性（登录账号与工号相同）
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, employeeNo);
        if (userRepository.selectCount(usernameWrapper) > 0) {
            throw new BusinessException(1002, "登录账号已存在（与工号相同），请使用其他工号");
        }

        // 校验枚举值
        validateDictValue("department", dto.getDepartment());
        validateDictValue("position", dto.getPosition());
        if (dto.getWorkStatus() != null) {
            validateDictValue("work_status", dto.getWorkStatus());
        }
        if (dto.getSkillLevel() != null) {
            validateDictValue("skill_level", dto.getSkillLevel());
        }
        if (dto.getShiftType() != null) {
            validateDictValue("shift_type", dto.getShiftType());
        }

        // 校验角色
        if (dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            throw new BusinessException(1003, "请至少选择一个角色");
        }

        // 校验仓库
        if (dto.getWarehouseIds() == null || dto.getWarehouseIds().isEmpty()) {
            throw new BusinessException(1004, "请至少选择一个仓库");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(employeeNo); // 登录账号自动设置为工号
        user.setEmployeeNo(employeeNo);
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setDepartment(dto.getDepartment());
        user.setPosition(dto.getPosition());
        user.setHireDate(dto.getHireDate());
        user.setWorkStatus(dto.getWorkStatus() != null ? dto.getWorkStatus() : SysUser.WORK_STATUS_ACTIVE);
        user.setSkillLevel(dto.getSkillLevel());
        user.setShiftType(dto.getShiftType());
        user.setStatus(SysUser.STATUS_ENABLED);

        // 设置密码
        String password = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(password));

        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        log.info("About to insert user: {}", user);
        int result = userRepository.insert(user);
        log.info("Insert result: {}, user id after insert: {}", result, user.getId());

        // 保存角色关联
        for (Long roleId : dto.getRoleIds()) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            userRole.setCreateTime(now);
            userRoleRepository.insert(userRole);
        }

        // 保存仓库关联
        for (Long warehouseId : dto.getWarehouseIds()) {
            SysUserWarehouse userWarehouse = new SysUserWarehouse();
            userWarehouse.setUserId(user.getId());
            userWarehouse.setWarehouseId(warehouseId);
            userWarehouse.setPermissionType(SysUserWarehouse.PERMISSION_OPERATE);
            userWarehouse.setCreateTime(now);
            userWarehouseRepository.insert(userWarehouse);
        }

        return user.getId();
    }

    @Transactional
    public void updateUser(Long id, UserUpdateDTO dto) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        // 更新基本信息
        if (StringUtils.hasText(dto.getName())) {
            if (dto.getName().length() > MAX_NAME_LENGTH) {
                throw new BusinessException(1005, "姓名不能超过" + MAX_NAME_LENGTH + "个字符");
            }
            user.setName(dto.getName());
        }

        if (dto.getPhone() != null) {
            if (StringUtils.hasText(dto.getPhone())) {
                validatePhoneFormat(dto.getPhone());
            }
            user.setPhone(dto.getPhone());
        }

        if (dto.getEmail() != null) {
            if (StringUtils.hasText(dto.getEmail())) {
                validateEmailFormat(dto.getEmail());
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getDepartment() != null) {
            validateDictValue("department", dto.getDepartment());
            user.setDepartment(dto.getDepartment());
        }

        if (dto.getPosition() != null) {
            validateDictValue("position", dto.getPosition());
            user.setPosition(dto.getPosition());
        }

        if (dto.getHireDate() != null) {
            user.setHireDate(dto.getHireDate());
        }

        if (dto.getWorkStatus() != null) {
            validateDictValue("work_status", dto.getWorkStatus());
            user.setWorkStatus(dto.getWorkStatus());

            // 离职时自动禁用账号
            if (dto.getWorkStatus() == SysUser.WORK_STATUS_RESIGNED) {
                user.setStatus(SysUser.STATUS_DISABLED);
            }
        }

        if (dto.getSkillLevel() != null) {
            validateDictValue("skill_level", dto.getSkillLevel());
            user.setSkillLevel(dto.getSkillLevel());
        }

        if (dto.getShiftType() != null) {
            validateDictValue("shift_type", dto.getShiftType());
            user.setShiftType(dto.getShiftType());
        }

        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);

        // 更新角色关联
        if (dto.getRoleIds() != null) {
            // 删除旧角色
            LambdaQueryWrapper<SysUserRole> roleDeleteWrapper = new LambdaQueryWrapper<>();
            roleDeleteWrapper.eq(SysUserRole::getUserId, id);
            userRoleRepository.delete(roleDeleteWrapper);

            // 插入新角色
            for (Long roleId : dto.getRoleIds()) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(id);
                userRole.setRoleId(roleId);
                userRole.setCreateTime(LocalDateTime.now());
                userRoleRepository.insert(userRole);
            }
        }

        // 更新仓库关联
        if (dto.getWarehouseIds() != null) {
            // 删除旧仓库
            LambdaQueryWrapper<SysUserWarehouse> warehouseDeleteWrapper = new LambdaQueryWrapper<>();
            warehouseDeleteWrapper.eq(SysUserWarehouse::getUserId, id);
            userWarehouseRepository.delete(warehouseDeleteWrapper);

            // 插入新仓库
            for (Long warehouseId : dto.getWarehouseIds()) {
                SysUserWarehouse userWarehouse = new SysUserWarehouse();
                userWarehouse.setUserId(id);
                userWarehouse.setWarehouseId(warehouseId);
                userWarehouse.setPermissionType(SysUserWarehouse.PERMISSION_OPERATE);
                userWarehouse.setCreateTime(LocalDateTime.now());
                userWarehouseRepository.insert(userWarehouse);
            }
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        // 删除用户
        userRepository.deleteById(id);

        // 删除角色关联
        LambdaQueryWrapper<SysUserRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysUserRole::getUserId, id);
        userRoleRepository.delete(roleWrapper);

        // 删除仓库关联
        LambdaQueryWrapper<SysUserWarehouse> warehouseWrapper = new LambdaQueryWrapper<>();
        warehouseWrapper.eq(SysUserWarehouse::getUserId, id);
        userWarehouseRepository.delete(warehouseWrapper);
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException(1006, "不能删除当前登录用户");
        }
        deleteUser(id);
    }

    @Transactional
    public void enableUser(Long id, Long currentUserId) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }
        if (id.equals(currentUserId)) {
            throw new BusinessException(1007, "不能启用/禁用当前登录账号");
        }

        user.setStatus(SysUser.STATUS_ENABLED);
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
            throw new BusinessException(1007, "不能启用/禁用当前登录账号");
        }

        user.setStatus(SysUser.STATUS_DISABLED);
        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);
    }

    @Transactional
    public String resetPassword(Long id) {
        SysUser user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(1001, "用户不存在");
        }

        // 生成随机密码（8位字母数字组合）
        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);

        return newPassword;
    }

    /**
     * 获取字典列表
     */
    public List<SysDict> getDictByType(String dictType) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getDictType, dictType);
        wrapper.eq(SysDict::getStatus, 1);
        wrapper.orderByAsc(SysDict::getSortOrder);
        return dictRepository.selectList(wrapper);
    }

    // ========== 私有方法 ==========

    private void validateRequiredFields(UserCreateDTO dto) {
        // 员工工号不再必填，系统会自动生成
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(1009, "请输入姓名");
        }
        if (dto.getDepartment() == null) {
            throw new BusinessException(1010, "请选择部门");
        }
        if (dto.getPosition() == null) {
            throw new BusinessException(1011, "请选择岗位");
        }
    }

    private void validateFieldLengths(UserCreateDTO dto) {
        if (dto.getEmployeeNo().length() > MAX_EMPLOYEE_NO_LENGTH) {
            throw new BusinessException(1012, "工号不能超过" + MAX_EMPLOYEE_NO_LENGTH + "个字符");
        }
        if (dto.getName().length() > MAX_NAME_LENGTH) {
            throw new BusinessException(1013, "姓名不能超过" + MAX_NAME_LENGTH + "个字符");
        }
        if (dto.getPhone() != null && dto.getPhone().length() > MAX_PHONE_LENGTH) {
            throw new BusinessException(1014, "手机号不能超过" + MAX_PHONE_LENGTH + "个字符");
        }
        if (dto.getEmail() != null && dto.getEmail().length() > MAX_EMAIL_LENGTH) {
            throw new BusinessException(1015, "邮箱不能超过" + MAX_EMAIL_LENGTH + "个字符");
        }
    }

    private void validateEmployeeNoFormat(String employeeNo) {
        if (!EMPLOYEE_NO_PATTERN.matcher(employeeNo).matches()) {
            throw new BusinessException(1016, "工号只能包含字母、数字和下划线");
        }
    }

    private void validatePhoneFormat(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(1017, "手机号格式不正确");
        }
    }

    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(1018, "邮箱格式不正确");
        }
    }

    private void validateDictValue(String dictType, Integer dictCode) {
        if (dictCode == null) return;

        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getDictType, dictType);
        wrapper.eq(SysDict::getDictCode, dictCode);
        wrapper.eq(SysDict::getStatus, 1);

        Long count = dictRepository.selectCount(wrapper);
        if (count == 0) {
            throw new BusinessException(1019, "无效的" + getDictTypeName(dictType) + "值");
        }
    }

    private String getDictTypeName(String dictType) {
        switch (dictType) {
            case "department": return "部门";
            case "position": return "岗位";
            case "work_status": return "工作状态";
            case "skill_level": return "技能等级";
            case "shift_type": return "班次类型";
            default: return dictType;
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private Map<String, Object> toMap(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("employeeNo", user.getEmployeeNo());
        map.put("name", user.getName());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("department", user.getDepartment());
        map.put("departmentName", getDictValueName("department", user.getDepartment()));
        map.put("position", user.getPosition());
        map.put("positionName", getDictValueName("position", user.getPosition()));
        map.put("hireDate", user.getHireDate());
        map.put("workStatus", user.getWorkStatus());
        map.put("workStatusName", getDictValueName("work_status", user.getWorkStatus()));
        map.put("skillLevel", user.getSkillLevel());
        map.put("skillLevelName", getDictValueName("skill_level", user.getSkillLevel()));
        map.put("shiftType", user.getShiftType());
        map.put("shiftTypeName", getDictValueName("shift_type", user.getShiftType()));
        map.put("status", user.getStatus());
        map.put("statusName", user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用");
        map.put("lastLoginTime", user.getLastLoginTime());
        map.put("lastLoginIp", user.getLastLoginIp());
        map.put("loginCount", user.getLoginCount());
        map.put("createTime", user.getCreateTime());
        return map;
    }

    private String getDictValueName(String dictType, Integer dictCode) {
        if (dictType == null || dictCode == null) return null;

        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getDictType, dictType);
        wrapper.eq(SysDict::getDictCode, dictCode);

        SysDict dict = dictRepository.selectOne(wrapper);
        return dict != null ? dict.getDictValue() : null;
    }

    /**
     * 自动生成员工工号
     * 格式：EMP + 4位递增数字，如 EMP0001、EMP0002
     * 会自动跳过已存在的工号
     */
    private String generateEmployeeNo() {
        // 查询所有以EMP开头的工号
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(SysUser::getEmployeeNo)
               .likeRight(SysUser::getEmployeeNo, "EMP");

        List<SysUser> users = userRepository.selectList(wrapper);

        // 收集所有已使用的工号数字
        Set<Integer> usedNumbers = new HashSet<>();
        int maxNumber = 0;

        for (SysUser user : users) {
            String employeeNo = user.getEmployeeNo();
            if (employeeNo != null && employeeNo.startsWith("EMP")) {
                try {
                    String numStr = employeeNo.substring(3);
                    int num = Integer.parseInt(numStr);
                    usedNumbers.add(num);
                    if (num > maxNumber) {
                        maxNumber = num;
                    }
                } catch (NumberFormatException e) {
                    // 忽略格式不正确的工号
                }
            }
        }

        // 从1开始查找第一个未使用的工号
        for (int i = 1; i <= maxNumber + 1; i++) {
            if (!usedNumbers.contains(i)) {
                return String.format("EMP%04d", i);
            }
        }

        // 理论上不会到这里，但作为兜底
        return String.format("EMP%04d", maxNumber + 1);
    }
}
