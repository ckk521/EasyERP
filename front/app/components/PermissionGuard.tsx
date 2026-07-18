/**
 * 权限守卫组件
 * 根据权限显示或隐藏子组件
 */

import React from 'react';
import { useCheckPermission, useCheckAnyPermission, useCheckAllPermissions } from '../hooks/usePermissions';

interface PermissionGuardProps {
  /** 单个权限编码 */
  permission?: string;
  /** 多个权限编码（OR关系） */
  permissions?: string[];
  /** 多个权限编码（AND关系） */
  allPermissions?: string[];
  /** 子组件 */
  children: React.ReactNode;
  /** 无权限时显示的替代内容 */
  fallback?: React.ReactNode;
}

/**
 * 权限守卫组件
 *
 * 使用示例：
 *
 * // 单个权限
 * <PermissionGuard permission="inbound:order:create">
 *   <Button>创建入库单</Button>
 * </PermissionGuard>
 *
 * // 多个权限（OR关系 - 有任意一个即可）
 * <PermissionGuard permissions={['inbound:order:edit', 'inbound:order:delete']}>
 *   <Button>操作</Button>
 * </PermissionGuard>
 *
 * // 多个权限（AND关系 - 需要全部权限）
 * <PermissionGuard allPermissions={['inbound:order:edit', 'inbound:order:approve']}>
 *   <Button>编辑并审核</Button>
 * </PermissionGuard>
 */
export function PermissionGuard({
  permission,
  permissions,
  allPermissions,
  children,
  fallback = null,
}: PermissionGuardProps): React.ReactElement | null {
  // 单个权限检查
  const hasSinglePermission = useCheckPermission(permission || '');

  // 多个权限OR检查
  const hasAnyPermission = useCheckAnyPermission(permissions || []);

  // 多个权限AND检查
  const hasAllPermissions = useCheckAllPermissions(allPermissions || []);

  // 判断是否有权限
  let hasAccess = false;

  if (permission) {
    hasAccess = hasSinglePermission;
  } else if (permissions && permissions.length > 0) {
    hasAccess = hasAnyPermission;
  } else if (allPermissions && allPermissions.length > 0) {
    hasAccess = hasAllPermissions;
  }

  return hasAccess ? <>{children}</> : <>{fallback}</>;
}

/**
 * 高阶组件：为组件添加权限控制
 *
 * 使用示例：
 * const CreateButton = withPermission(Button, 'inbound:order:create');
 * <CreateButton>创建入库单</CreateButton>
 */
export function withPermission<P extends object>(
  Component: React.ComponentType<P>,
  permission: string,
  fallback?: React.ReactNode
) {
  return function WithPermissionComponent(props: P) {
    return (
      <PermissionGuard permission={permission} fallback={fallback}>
        <Component {...props} />
      </PermissionGuard>
    );
  };
}

/**
 * 高阶组件：为组件添加多权限控制（OR关系）
 */
export function withAnyPermission<P extends object>(
  Component: React.ComponentType<P>,
  permissions: string[],
  fallback?: React.ReactNode
) {
  return function WithAnyPermissionComponent(props: P) {
    return (
      <PermissionGuard permissions={permissions} fallback={fallback}>
        <Component {...props} />
      </PermissionGuard>
    );
  };
}

/**
 * 高阶组件：为组件添加多权限控制（AND关系）
 */
export function withAllPermissions<P extends object>(
  Component: React.ComponentType<P>,
  permissions: string[],
  fallback?: React.ReactNode
) {
  return function WithAllPermissionsComponent(props: P) {
    return (
      <PermissionGuard allPermissions={permissions} fallback={fallback}>
        <Component {...props} />
      </PermissionGuard>
    );
  };
}
