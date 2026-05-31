package com.wms.stocktake.controller;

import com.wms.stocktake.dto.*;
import com.wms.stocktake.entity.StocktakeItem;
import com.wms.stocktake.service.StocktakeService;
import com.wms.system.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 盘点控制器
 * Story 4.4-4.7 盘点管理API
 */
@RestController
@RequestMapping("/api/stocktake")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 从请求头提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Story 4.4: 创建盘点单
     * POST /api/stocktake/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody StocktakeCreateDTO dto, HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);

            Long orderId = stocktakeService.createOrder(dto, userId, username);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "盘点单创建成功",
                "data", Map.of("orderId", orderId)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "创建盘点单失败：" + e.getMessage()
            ));
        }
    }

    /**
     * Story 4.5: 盘点作业
     * POST /api/stocktake/count
     */
    @PostMapping("/count")
    public ResponseEntity<?> countItem(@RequestBody StocktakeCountDTO dto, HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);

            stocktakeService.countItem(dto, userId, username);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "盘点录入成功"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "盘点录入失败：" + e.getMessage()
            ));
        }
    }

    /**
     * Story 4.6: 完成盘点
     * POST /api/stocktake/finish/{orderId}
     */
    @PostMapping("/finish/{orderId}")
    public ResponseEntity<?> finishOrder(@PathVariable Long orderId) {
        try {
            stocktakeService.finishOrder(orderId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "盘点已完成，进入审核状态"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "完成盘点失败：" + e.getMessage()
            ));
        }
    }

    /**
     * Story 4.7: 查询盘点单列表
     * GET /api/stocktake/list
     */
    @GetMapping("/list")
    public ResponseEntity<?> queryOrders(StocktakeQueryDTO query) {
        try {
            Map<String, Object> result = stocktakeService.queryOrders(query);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "查询失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 获取盘点单详情
     * GET /api/stocktake/detail/{orderId}
     */
    @GetMapping("/detail/{orderId}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long orderId) {
        try {
            Map<String, Object> result = stocktakeService.getOrderDetail(orderId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "查询详情失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 获取盘点明细列表
     * GET /api/stocktake/items/{orderId}
     */
    @GetMapping("/items/{orderId}")
    public ResponseEntity<?> getOrderItems(@PathVariable Long orderId) {
        try {
            List<StocktakeItem> items = stocktakeService.getOrderItems(orderId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", items
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "查询明细失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 取消盘点单
     * POST /api/stocktake/cancel/{orderId}
     */
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            stocktakeService.cancelOrder(orderId, reason);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "盘点单已取消"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "取消失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 强制取消盘点单（可取消任意非完成状态）
     * POST /api/stocktake/force-cancel/{orderId}
     */
    @PostMapping("/force-cancel/{orderId}")
    public ResponseEntity<?> forceCancelOrder(@PathVariable Long orderId, @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            stocktakeService.forceCancelOrder(orderId, reason);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "盘点单已强制取消"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "取消失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 手动触发生成循环盘数据
     * POST /api/stocktake/generate-cycle-data/{orderId}
     */
    @PostMapping("/generate-cycle-data/{orderId}")
    public ResponseEntity<?> generateCycleData(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);

            stocktakeService.generateCycleData(orderId, userId, username);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "盘点数据生成成功"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "生成失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 审核盘点单（通过）
     * POST /api/stocktake/approve/{orderId}
     */
    @PostMapping("/approve/{orderId}")
    public ResponseEntity<?> approveOrder(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);

            stocktakeService.approveOrder(orderId, userId, username);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "审核通过，库存已调整"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "审核失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 审核盘点单（驳回）
     * POST /api/stocktake/reject/{orderId}
     */
    @PostMapping("/reject/{orderId}")
    public ResponseEntity<?> rejectOrder(@PathVariable Long orderId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);
            String reason = body.get("reason");

            stocktakeService.rejectOrder(orderId, reason, userId, username);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已驳回，可重新盘点"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "驳回失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 查询审核记录
     * GET /api/stocktake/approve-records/{orderId}
     */
    @GetMapping("/approve-records/{orderId}")
    public ResponseEntity<?> getApproveRecords(@PathVariable Long orderId) {
        try {
            List<Map<String, Object>> records = stocktakeService.getApproveRecords(orderId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", records
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "查询失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 执行数据库迁移（临时接口）
     * POST /api/stocktake/migrate
     */
    @PostMapping("/migrate")
    public ResponseEntity<?> migrate() {
        try {
            // 创建审核记录表
            String createApproveRecordTable = "CREATE TABLE IF NOT EXISTS wms_stocktake_approve_record (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "order_id BIGINT NOT NULL COMMENT '盘点单ID', " +
                "order_no VARCHAR(20) NOT NULL COMMENT '盘点单号', " +
                "action VARCHAR(20) NOT NULL COMMENT '操作类型: approve通过/reject驳回', " +
                "reason TEXT NULL COMMENT '驳回原因', " +
                "operator_id BIGINT NOT NULL COMMENT '操作人ID', " +
                "operator_name VARCHAR(50) NOT NULL COMMENT '操作人姓名', " +
                "operator_role_name VARCHAR(50) NULL COMMENT '操作人角色名称', " +
                "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间', " +
                "INDEX idx_order_id (order_id), " +
                "INDEX idx_order_no (order_no)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点审核记录表'";
            try {
                jdbcTemplate.execute(createApproveRecordTable);
            } catch (Exception ignored) {
                // 表可能已存在
            }

            // 添加角色名称字段（如果表已存在但缺少该字段）
            try {
                jdbcTemplate.execute("ALTER TABLE wms_stocktake_approve_record ADD COLUMN operator_role_name VARCHAR(50) NULL COMMENT '操作人角色名称' AFTER operator_name");
            } catch (Exception ignored) {
                // 字段可能已存在
            }

            // 逐个添加字段（MySQL不支持IF NOT EXISTS）
            String[] alterSqls = {
                "ALTER TABLE wms_stocktake_order ADD COLUMN cycle_type VARCHAR(20) NULL COMMENT '周期类型' AFTER remark",
                "ALTER TABLE wms_stocktake_order ADD COLUMN cycle_day INT NULL COMMENT '盘点日' AFTER cycle_type",
                "ALTER TABLE wms_stocktake_order ADD COLUMN cycle_strategy VARCHAR(30) NULL COMMENT '轮转策略' AFTER cycle_day",
                "ALTER TABLE wms_stocktake_order ADD COLUMN cycle_config TEXT NULL COMMENT '轮转配置' AFTER cycle_strategy",
                "ALTER TABLE wms_stocktake_order ADD COLUMN cycle_index INT DEFAULT 0 COMMENT '轮转索引' AFTER cycle_config",
                "ALTER TABLE wms_stocktake_order ADD COLUMN last_cycle_date DATE NULL COMMENT '上次执行日期' AFTER cycle_index",
                "ALTER TABLE wms_stocktake_order ADD COLUMN next_cycle_date DATE NULL COMMENT '下次执行日期' AFTER last_cycle_date",
                "ALTER TABLE wms_stocktake_order ADD COLUMN parent_strategy_id BIGINT NULL COMMENT '父策略ID' AFTER next_cycle_date"
            };

            for (String sql : alterSqls) {
                try {
                    jdbcTemplate.execute(sql);
                } catch (Exception ignored) {
                    // 字段可能已存在，忽略错误
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "数据库迁移完成"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "迁移完成：" + e.getMessage()
            ));
        }
    }
}
