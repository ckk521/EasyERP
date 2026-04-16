package com.wms.system.controller;

import com.wms.system.common.Result;
import com.wms.system.dto.LoginDTO;
import com.wms.system.entity.SysUser;
import com.wms.system.repository.SysUserRepository;
import com.wms.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        Map<String, Object> data = authService.login(dto);
        return Result.success("登录成功", data);
    }

    // TEMP: Setup endpoint for testing - resets admin password to admin123
    @PostMapping("/setup")
    public Result<String> setup() {
        SysUser admin = userRepository.selectById(1L);
        if (admin != null) {
            admin.setPassword(passwordEncoder.encode("admin123"));
            userRepository.updateById(admin);
            return Result.success("Admin password reset to admin123", null);
        }
        return Result.error(404, "Admin user not found");
    }
}
