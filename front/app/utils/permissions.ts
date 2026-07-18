/**
 * 权限工具函数
 * 用于前端权限判断和控制
 */

import { useMemo } from 'react';

// 用户权限缓存
let cachedPermissions: Set<string> | null = null;
let cachedMenuPermissions: string[] | null = null;

/**
 * 设置用户权限（登录后调用）
 */
export function setUserPermissions(permissions: string[]): void {
  cachedPermissions = new Set(permissions);
  cachedMenuPermissions = permissions.filter(p => p.endsWith(':menu'));
}

/**
 * 获取用户权限
 */
export function getUserPermissions(): Set<string> {
  return cachedPermissions || new Set();
}

/**
 * 获取菜单权限
 */
export function getMenuPermissions(): string[] {
  return cachedMenuPermissions || [];
}

/**
 * 清除权限缓存（登出时调用）
 */
export function clearPermissions(): void {
  cachedPermissions = null;
  cachedMenuPermissions = null;
}

/**
 * 检查是否有单个权限
 */
export function hasPermission(permission: string): boolean {
  if (!cachedPermissions) return false;
  return cachedPermissions.has(permission);
}

/**
 * 检查是否有任意一个权限（OR关系）
 */
export function hasAnyPermission(permissions: string[]): boolean {
  if (!cachedPermissions) return false;
  return permissions.some(p => cachedPermissions!.has(p));
}

/**
 * 检查是否有所有权限（AND关系）
 */
export function hasAllPermissions(permissions: string[]): boolean {
  if (!cachedPermissions) return false;
  return permissions.every(p => cachedPermissions!.has(p));
}

/**
 * 检查菜单权限
 */
export function hasMenuPermission(menuKey: string): boolean {
  const menuPermission = `${menuKey}:menu`;
  return hasPermission(menuPermission);
}

/**
 * React Hook: 检查权限
 */
export function usePermission(permission: string): boolean {
  return useMemo(() => hasPermission(permission), [permission]);
}

/**
 * React Hook: 检查任意权限
 */
export function useAnyPermission(permissions: string[]): boolean {
  return useMemo(() => hasAnyPermission(permissions), [permissions]);
}

/**
 * React Hook: 检查所有权限
 */
export function useAllPermissions(permissions: string[]): boolean {
  return useMemo(() => hasAllPermissions(permissions), [permissions]);
}

/**
 * 权限编码常量（与后端保持一致）
 */
export const PERMISSIONS = {
  // 仪表盘
  DASHBOARD_MENU: 'dashboard:menu',
  DASHBOARD_VIEW: 'dashboard:view',

  // 系统管理
  SYSTEM_MENU: 'system:menu',
  SYSTEM_USER_VIEW: 'system:user:view',
  SYSTEM_USER_CREATE: 'system:user:create',
  SYSTEM_USER_EDIT: 'system:user:edit',
  SYSTEM_USER_DELETE: 'system:user:delete',
  SYSTEM_USER_RESET_PWD: 'system:user:resetPwd',
  SYSTEM_ROLE_VIEW: 'system:role:view',
  SYSTEM_DICT_VIEW: 'system:dict:view',

  // 入库管理
  INBOUND_MENU: 'inbound:menu',
  INBOUND_ORDER_VIEW: 'inbound:order:view',
  INBOUND_ORDER_CREATE: 'inbound:order:create',
  INBOUND_ORDER_EDIT: 'inbound:order:edit',
  INBOUND_ORDER_DELETE: 'inbound:order:delete',
  INBOUND_ORDER_APPROVE: 'inbound:order:approve',
  INBOUND_ORDER_EXPORT: 'inbound:order:export',
  INBOUND_RECEIVE_VIEW: 'inbound:receive:view',
  INBOUND_RECEIVE_OPERATE: 'inbound:receive:operate',
  INBOUND_INSPECT_VIEW: 'inbound:inspect:view',
  INBOUND_INSPECT_OPERATE: 'inbound:inspect:operate',
  INBOUND_PUTAWAY_VIEW: 'inbound:putaway:view',
  INBOUND_PUTAWAY_OPERATE: 'inbound:putaway:operate',

  // 出库管理
  OUTBOUND_MENU: 'outbound:menu',
  OUTBOUND_ORDER_VIEW: 'outbound:order:view',
  OUTBOUND_ORDER_CREATE: 'outbound:order:create',
  OUTBOUND_ORDER_EDIT: 'outbound:order:edit',
  OUTBOUND_ORDER_DELETE: 'outbound:order:delete',
  OUTBOUND_ORDER_APPROVE: 'outbound:order:approve',
  OUTBOUND_ORDER_EXPORT: 'outbound:order:export',
  OUTBOUND_PICK_VIEW: 'outbound:pick:view',
  OUTBOUND_PICK_OPERATE: 'outbound:pick:operate',
  OUTBOUND_PACK_VIEW: 'outbound:pack:view',
  OUTBOUND_PACK_OPERATE: 'outbound:pack:operate',
  OUTBOUND_SHIP_VIEW: 'outbound:ship:view',
  OUTBOUND_SHIP_OPERATE: 'outbound:ship:operate',

  // 库存管理
  INVENTORY_MENU: 'inventory:menu',
  INVENTORY_QUERY_VIEW: 'inventory:query:view',
  INVENTORY_STOCKTAKE_VIEW: 'inventory:stocktake:view',
  INVENTORY_STOCKTAKE_CREATE: 'inventory:stocktake:create',
  INVENTORY_STOCKTAKE_EDIT: 'inventory:stocktake:edit',
  INVENTORY_STOCKTAKE_DELETE: 'inventory:stocktake:delete',
  INVENTORY_STOCKTAKE_OPERATE: 'inventory:stocktake:operate',
  INVENTORY_STOCKTAKE_APPROVE: 'inventory:stocktake:approve',
  INVENTORY_TRANSFER_VIEW: 'inventory:transfer:view',
  INVENTORY_TRANSFER_CREATE: 'inventory:transfer:create',
  INVENTORY_TRANSFER_APPROVE: 'inventory:transfer:approve',

  // 退换货
  RETURN_MENU: 'return:menu',
  RETURN_ORDER_VIEW: 'return:order:view',
  RETURN_ORDER_CREATE: 'return:order:create',
  RETURN_ORDER_EDIT: 'return:order:edit',
  RETURN_ORDER_DELETE: 'return:order:delete',
  RETURN_ORDER_APPROVE: 'return:order:approve',

  // 报表
  REPORT_MENU: 'report:menu',
  REPORT_INBOUND_VIEW: 'report:inbound:view',
  REPORT_OUTBOUND_VIEW: 'report:outbound:view',
  REPORT_INVENTORY_VIEW: 'report:inventory:view',
  REPORT_PERFORMANCE_VIEW: 'report:performance:view',
} as const;
