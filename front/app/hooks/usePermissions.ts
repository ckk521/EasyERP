/**
 * 权限管理Hook
 * 提供权限获取、缓存和判断功能
 */

import { useState, useEffect, useCallback } from 'react';
import { setUserPermissions, clearPermissions, getUserPermissions } from '../utils/permissions';

// API地址
const API_BASE = '/api/v1/system';

// 权限状态
interface PermissionState {
  permissions: Set<string>;
  menuPermissions: string[];
  loading: boolean;
  error: string | null;
}

// 全局状态
let globalState: PermissionState = {
  permissions: new Set(),
  menuPermissions: [],
  loading: false,
  error: null,
};

// 订阅者列表
const subscribers = new Set<() => void>();

// 通知所有订阅者
function notifySubscribers() {
  subscribers.forEach(callback => callback());
}

/**
 * 获取用户权限
 */
async function fetchUserPermissions(): Promise<{ permissions: string[] }> {
  const response = await fetch(`${API_BASE}/permissions/user`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
    },
  });

  if (!response.ok) {
    throw new Error('获取权限失败');
  }

  const result = await response.json();
  return { permissions: result.data || [] };
}

/**
 * 获取菜单权限
 */
async function fetchMenuPermissions(): Promise<{ menus: string[] }> {
  const response = await fetch(`${API_BASE}/permissions/menus`, {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
    },
  });

  if (!response.ok) {
    throw new Error('获取菜单权限失败');
  }

  const result = await response.json();
  return { menus: result.data || [] };
}

/**
 * 初始化权限（登录后调用）
 */
export async function initPermissions(): Promise<void> {
  globalState = { ...globalState, loading: true, error: null };
  notifySubscribers();

  try {
    // 并行获取权限和菜单
    const [permissionResult, menuResult] = await Promise.all([
      fetchUserPermissions(),
      fetchMenuPermissions(),
    ]);

    const permissions = permissionResult.permissions;
    const menuPermissions = menuResult.menus;

    // 更新缓存
    setUserPermissions(permissions);

    // 更新状态
    globalState = {
      permissions: new Set(permissions),
      menuPermissions,
      loading: false,
      error: null,
    };
  } catch (error) {
    globalState = {
      ...globalState,
      loading: false,
      error: error instanceof Error ? error.message : '获取权限失败',
    };
  }

  notifySubscribers();
}

/**
 * 清除权限（登出时调用）
 */
export function clearAllPermissions(): void {
  clearPermissions();
  globalState = {
    permissions: new Set(),
    menuPermissions: [],
    loading: false,
    error: null,
  };
  notifySubscribers();
}

/**
 * 刷新权限
 */
export async function refreshPermissions(): Promise<void> {
  await initPermissions();
}

/**
 * 使用权限状态Hook
 */
export function usePermissionState(): PermissionState {
  const [state, setState] = useState<PermissionState>(globalState);

  useEffect(() => {
    const callback = () => setState({ ...globalState });
    subscribers.add(callback);
    return () => {
      subscribers.delete(callback);
    };
  }, []);

  return state;
}

/**
 * 使用权限加载Hook
 */
export function usePermissionLoader() {
  const loadPermissions = useCallback(async () => {
    await initPermissions();
  }, []);

  const clear = useCallback(() => {
    clearAllPermissions();
  }, []);

  const refresh = useCallback(async () => {
    await refreshPermissions();
  }, []);

  return { loadPermissions, clear, refresh };
}

/**
 * 检查权限Hook
 */
export function useCheckPermission(permission: string): boolean {
  const state = usePermissionState();
  return state.permissions.has(permission);
}

/**
 * 检查任意权限Hook
 */
export function useCheckAnyPermission(permissions: string[]): boolean {
  const state = usePermissionState();
  return permissions.some(p => state.permissions.has(p));
}

/**
 * 检查所有权限Hook
 */
export function useCheckAllPermissions(permissions: string[]): boolean {
  const state = usePermissionState();
  return permissions.every(p => state.permissions.has(p));
}

/**
 * 获取菜单权限Hook
 */
export function useMenuPermissions(): string[] {
  const state = usePermissionState();
  return state.menuPermissions;
}

/**
 * 检查菜单权限Hook
 */
export function useCheckMenuPermission(menuKey: string): boolean {
  const state = usePermissionState();
  const menuPermission = `${menuKey}:menu`;
  return state.permissions.has(menuPermission);
}
