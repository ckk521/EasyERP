package com.wms.system.aspect;

import com.wms.system.annotation.OperationLog;
import com.wms.system.entity.SysOperationLog;
import com.wms.system.entity.SysUser;
import com.wms.system.repository.SysOperationLogRepository;
import com.wms.system.repository.SysUserRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 自动记录带有 @OperationLog 注解的方法调用
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogRepository operationLogRepository;
    private final SysUserRepository userRepository;

    @Around("@annotation(com.wms.system.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        // 获取请求信息
        HttpServletRequest request = getRequest();
        String ip = getClientIp(request);
        String requestUrl = request != null ? request.getRequestURI() : "";
        String requestMethod = request != null ? request.getMethod() : "";
        String requestParams = getRequestParams(joinPoint);

        // 获取当前登录用户
        SysUser currentUser = getCurrentUser();

        // 构建日志对象
        SysOperationLog sysLog = new SysOperationLog();
        sysLog.setModule(operationLog.module());
        sysLog.setAction(operationLog.action());
        sysLog.setTime(LocalDateTime.now());
        sysLog.setIp(ip);
        sysLog.setRequestMethod(requestMethod);
        sysLog.setRequestUrl(requestUrl);
        sysLog.setRequestParams(truncateParams(requestParams));

        if (currentUser != null) {
            sysLog.setUserId(currentUser.getId());
            sysLog.setUsername(currentUser.getUsername());
            sysLog.setRealName(currentUser.getName());
        }

        Object result = null;
        String resultStr = "成功";
        String errorMsg = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            resultStr = "成功";
        } catch (Exception e) {
            resultStr = "失败";
            errorMsg = e.getMessage();
            log.error("操作失败: {} - {}", operationLog.module(), e.getMessage());
            throw e;
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            sysLog.setResult(resultStr);
            sysLog.setErrorMessage(errorMsg);
            sysLog.setResponseTime(responseTime);

            // 设置操作对象（从方法参数中提取）
            sysLog.setObject(extractTargetObject(joinPoint, method, result));

            // 异步保存日志（不阻塞主流程）
            try {
                sysLog.setCreatedAt(LocalDateTime.now());
                operationLogRepository.insert(sysLog);
                log.debug("操作日志记录成功: {} - {} - {}ms", sysLog.getModule(), sysLog.getAction(), responseTime);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }

        return result;
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private SysUser getCurrentUser() {
        try {
            // 从请求属性中获取用户信息（由 JwtAuthenticationFilter 设置）
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String username = (String) request.getAttribute("username");
                if (username != null) {
                    LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(SysUser::getUsername, username);
                    return userRepository.selectOne(wrapper);
                }
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败", e);
        }
        return null;
    }

    private String getRequestParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg == null) continue;
                if (arg instanceof HttpServletRequest || arg instanceof javax.servlet.http.HttpServletResponse) {
                    continue;
                }
                sb.append(arg.toString()).append("; ");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String truncateParams(String params) {
        if (params == null) return "";
        return params.length() > 2000 ? params.substring(0, 2000) : params;
    }

    private String extractTargetObject(ProceedingJoinPoint joinPoint, Method method, Object result) {
        try {
            // 尝试从方法返回值中提取对象标识
            if (result != null) {
                // 常见的对象标识字段
                String[] idFields = {"id", "code", "name", "username"};
                for (String field : idFields) {
                    try {
                        java.lang.reflect.Field f = result.getClass().getDeclaredField(field);
                        f.setAccessible(true);
                        Object value = f.get(result);
                        if (value != null) {
                            return field + "=" + value;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.debug("提取目标对象失败", e);
        }
        return "";
    }
}