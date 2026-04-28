package com.wms.system.util;

import lombok.Data;

/**
 * 当前登录用户上下文
 */
@Data
public class UserContext {
    private Long userId;
    private String username;

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Long getCurrentUserId() {
        UserContext context = get();
        return context != null ? context.getUserId() : null;
    }

    public static String getCurrentUsername() {
        UserContext context = get();
        return context != null ? context.getUsername() : null;
    }

    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }
}
