package com.wms.system.constant;

/**
 * 权限编码常量类
 * 避免权限编码硬编码，便于维护
 */
public class PermissionConstants {

    // ========== 仪表盘 ==========
    public static final String DASHBOARD_MENU = "dashboard:menu";
    public static final String DASHBOARD_VIEW = "dashboard:view";

    // ========== 系统管理 ==========
    public static final String SYSTEM_MENU = "system:menu";
    public static final String SYSTEM_USER_VIEW = "system:user:view";
    public static final String SYSTEM_USER_CREATE = "system:user:create";
    public static final String SYSTEM_USER_EDIT = "system:user:edit";
    public static final String SYSTEM_USER_DELETE = "system:user:delete";
    public static final String SYSTEM_USER_RESET_PWD = "system:user:resetPwd";
    public static final String SYSTEM_ROLE_VIEW = "system:role:view";
    public static final String SYSTEM_DICT_VIEW = "system:dict:view";

    // ========== 入库管理 ==========
    public static final String INBOUND_MENU = "inbound:menu";
    public static final String INBOUND_ORDER_VIEW = "inbound:order:view";
    public static final String INBOUND_ORDER_CREATE = "inbound:order:create";
    public static final String INBOUND_ORDER_EDIT = "inbound:order:edit";
    public static final String INBOUND_ORDER_DELETE = "inbound:order:delete";
    public static final String INBOUND_ORDER_APPROVE = "inbound:order:approve";
    public static final String INBOUND_ORDER_EXPORT = "inbound:order:export";
    public static final String INBOUND_RECEIVE_VIEW = "inbound:receive:view";
    public static final String INBOUND_RECEIVE_OPERATE = "inbound:receive:operate";
    public static final String INBOUND_INSPECT_VIEW = "inbound:inspect:view";
    public static final String INBOUND_INSPECT_OPERATE = "inbound:inspect:operate";
    public static final String INBOUND_PUTAWAY_VIEW = "inbound:putaway:view";
    public static final String INBOUND_PUTAWAY_OPERATE = "inbound:putaway:operate";

    // ========== 出库管理 ==========
    public static final String OUTBOUND_MENU = "outbound:menu";
    public static final String OUTBOUND_ORDER_VIEW = "outbound:order:view";
    public static final String OUTBOUND_ORDER_CREATE = "outbound:order:create";
    public static final String OUTBOUND_ORDER_EDIT = "outbound:order:edit";
    public static final String OUTBOUND_ORDER_DELETE = "outbound:order:delete";
    public static final String OUTBOUND_ORDER_APPROVE = "outbound:order:approve";
    public static final String OUTBOUND_ORDER_EXPORT = "outbound:order:export";
    public static final String OUTBOUND_PICK_VIEW = "outbound:pick:view";
    public static final String OUTBOUND_PICK_OPERATE = "outbound:pick:operate";
    public static final String OUTBOUND_PACK_VIEW = "outbound:pack:view";
    public static final String OUTBOUND_PACK_OPERATE = "outbound:pack:operate";
    public static final String OUTBOUND_SHIP_VIEW = "outbound:ship:view";
    public static final String OUTBOUND_SHIP_OPERATE = "outbound:ship:operate";

    // ========== 库存管理 ==========
    public static final String INVENTORY_MENU = "inventory:menu";
    public static final String INVENTORY_QUERY_VIEW = "inventory:query:view";
    public static final String INVENTORY_STOCKTAKE_VIEW = "inventory:stocktake:view";
    public static final String INVENTORY_STOCKTAKE_CREATE = "inventory:stocktake:create";
    public static final String INVENTORY_STOCKTAKE_EDIT = "inventory:stocktake:edit";
    public static final String INVENTORY_STOCKTAKE_DELETE = "inventory:stocktake:delete";
    public static final String INVENTORY_STOCKTAKE_OPERATE = "inventory:stocktake:operate";
    public static final String INVENTORY_STOCKTAKE_APPROVE = "inventory:stocktake:approve";
    public static final String INVENTORY_TRANSFER_VIEW = "inventory:transfer:view";
    public static final String INVENTORY_TRANSFER_CREATE = "inventory:transfer:create";
    public static final String INVENTORY_TRANSFER_APPROVE = "inventory:transfer:approve";

    // ========== 退换货管理 ==========
    public static final String RETURN_MENU = "return:menu";
    public static final String RETURN_ORDER_VIEW = "return:order:view";
    public static final String RETURN_ORDER_CREATE = "return:order:create";
    public static final String RETURN_ORDER_EDIT = "return:order:edit";
    public static final String RETURN_ORDER_DELETE = "return:order:delete";
    public static final String RETURN_ORDER_APPROVE = "return:order:approve";

    // ========== 报表分析 ==========
    public static final String REPORT_MENU = "report:menu";
    public static final String REPORT_INBOUND_VIEW = "report:inbound:view";
    public static final String REPORT_OUTBOUND_VIEW = "report:outbound:view";
    public static final String REPORT_INVENTORY_VIEW = "report:inventory:view";
    public static final String REPORT_PERFORMANCE_VIEW = "report:performance:view";
}