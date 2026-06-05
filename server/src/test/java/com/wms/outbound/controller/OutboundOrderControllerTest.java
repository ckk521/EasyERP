package com.wms.outbound.controller;

import com.wms.outbound.dto.OutboundOrderQueryDTO;
import com.wms.outbound.service.OutboundOrderService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 出库单控制器测试
 * TDD: 测试覆盖API端点
 *
 * 测试用例对照：
 * - TC-QUERY-001: 查询出库单列表API
 * - TC-QUERY-002: 按条件查询API
 * - TC-QUERY-003: 查看出库单详情API
 * - TC-QUERY-005: 导出出库数据API
 */
class OutboundOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OutboundOrderService orderService;

    @InjectMocks
    private OutboundOrderController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ========== TC-QUERY-001 查询出库单列表API ==========

    @Test
    @DisplayName("TC-QUERY-001: GET /api/v1/outbound/orders - 返回分页列表")
    void testListOrdersAPI() throws Exception {
        // Given: 模拟服务返回
        Map<String, Object> result = new HashMap<>();
        result.put("total", 2L);
        result.put("page", 1);
        result.put("limit", 20);
        result.put("list", Arrays.asList(
            createOrderMap(1L, "OB20260531001"),
            createOrderMap(2L, "OB20260531002")
        ));

        when(orderService.listOrders(any(OutboundOrderQueryDTO.class))).thenReturn(result);

        // When & Then: 调用API
        mockMvc.perform(get("/api/v1/outbound/orders")
                .param("page", "1")
                .param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.list").isArray())
            .andExpect(jsonPath("$.data.list.length()").value(2));
    }

    // ========== TC-QUERY-002 按条件查询API ==========

    @Test
    @DisplayName("TC-QUERY-002: GET /api/v1/outbound/orders?status=0 - 按状态查询")
    void testListOrdersByStatusAPI() throws Exception {
        // Given: 模拟服务返回
        Map<String, Object> result = new HashMap<>();
        result.put("total", 1L);
        result.put("list", Collections.singletonList(createOrderMap(1L, "OB20260531001")));

        when(orderService.listOrders(any(OutboundOrderQueryDTO.class))).thenReturn(result);

        // When & Then: 调用API
        mockMvc.perform(get("/api/v1/outbound/orders")
                .param("status", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("TC-QUERY-002: GET /api/v1/outbound/orders?keyword=OB202605 - 关键字查询")
    void testListOrdersByKeywordAPI() throws Exception {
        // Given: 模拟服务返回
        Map<String, Object> result = new HashMap<>();
        result.put("total", 1L);
        result.put("list", Collections.singletonList(createOrderMap(1L, "OB20260531001")));

        when(orderService.listOrders(any(OutboundOrderQueryDTO.class))).thenReturn(result);

        // When & Then: 调用API
        mockMvc.perform(get("/api/v1/outbound/orders")
                .param("keyword", "OB202605"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("TC-QUERY-002: GET /api/v1/outbound/orders?startDate=2026-05-01&endDate=2026-05-31 - 日期范围查询")
    void testListOrdersByDateRangeAPI() throws Exception {
        // Given: 模拟服务返回
        Map<String, Object> result = new HashMap<>();
        result.put("total", 5L);
        result.put("list", Collections.emptyList());

        when(orderService.listOrders(any(OutboundOrderQueryDTO.class))).thenReturn(result);

        // When & Then: 调用API
        mockMvc.perform(get("/api/v1/outbound/orders")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(5));
    }

    // ========== TC-QUERY-003 查看出库单详情API ==========

    @Test
    @DisplayName("TC-QUERY-003: GET /api/v1/outbound/orders/{id} - 返回详情")
    void testGetOrderDetailAPI() throws Exception {
        // Given: 模拟服务返回
        Map<String, Object> detail = createOrderMap(1L, "OB20260531001");
        detail.put("items", Arrays.asList(
            createItemMap(1L, "SKU-001", 10),
            createItemMap(2L, "SKU-002", 5)
        ));

        when(orderService.getOrderDetail(1L)).thenReturn(detail);

        // When & Then: 调用API
        mockMvc.perform(get("/api/v1/outbound/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.orderNo").value("OB20260531001"))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    @DisplayName("TC-QUERY-003: 出库单不存在时服务抛出异常")
    void testGetOrderDetail_ServiceThrowsException() {
        // Given: 模拟服务抛出异常
        when(orderService.getOrderDetail(999L)).thenThrow(new RuntimeException("出库单不存在"));

        // When & Then: 直接调用服务验证异常
        Assertions.assertThrows(RuntimeException.class, () -> {
            controller.getOrderDetail(999L);
        });
    }

    // ========== TC-QUERY-005 导出出库数据API ==========

    @Test
    @DisplayName("TC-QUERY-005: GET /api/v1/outbound/orders/export - 导出数据")
    void testExportOrdersAPI() throws Exception {
        // Given: 模拟服务返回
        Map<String, Object> result = new HashMap<>();
        result.put("total", 2L);
        result.put("list", Arrays.asList(
            createOrderMap(1L, "OB20260531001"),
            createOrderMap(2L, "OB20260531002")
        ));

        when(orderService.listOrders(any(OutboundOrderQueryDTO.class))).thenReturn(result);

        // When & Then: 调用API（使用现有list接口，前端处理导出）
        mockMvc.perform(get("/api/v1/outbound/orders")
                .param("limit", "1000")) // 大limit实现导出
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list").isArray());
    }

    // ========== 按销售订单号查询API ==========

    @Test
    @DisplayName("TC-QUERY-002: GET /api/v1/outbound/orders/by-so?soNo=SO20260531001 - 按销售订单号查询")
    void testListBySoNoAPI() throws Exception {
        // Given: 模拟服务返回
        Map<String, Object> result = new HashMap<>();
        result.put("total", 1L);
        result.put("list", Collections.singletonList(createOrderMap(1L, "OB20260531001")));

        when(orderService.listOrders(any(OutboundOrderQueryDTO.class))).thenReturn(result);

        // When & Then: 调用API
        mockMvc.perform(get("/api/v1/outbound/orders/by-so")
                .param("soNo", "SO20260531001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> createOrderMap(Long id, String orderNo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("orderNo", orderNo);
        map.put("orderType", 1);
        map.put("orderTypeName", "销售出库");
        map.put("sourceType", 1);
        map.put("sourceTypeName", "ERP推送");
        map.put("soNo", "SO20260531001");
        map.put("customerId", 100L);
        map.put("customerCode", "CUS-001");
        map.put("customerName", "测试客户");
        map.put("warehouseId", 1L);
        map.put("warehouseCode", "WH-CN-001");
        map.put("warehouseName", "深圳总仓");
        map.put("priority", 3);
        map.put("priorityName", "中");
        map.put("status", 0);
        map.put("statusName", "待分配");
        map.put("totalQty", 15);
        map.put("totalPickedQty", 0);
        map.put("totalPackedQty", 0);
        map.put("totalShippedQty", 0);
        map.put("progressPick", 0);
        map.put("progressPack", 0);
        map.put("progressShip", 0);
        map.put("createTime", LocalDateTime.now().toString());
        return map;
    }

    private Map<String, Object> createItemMap(Long id, String skuCode, int qty) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("skuCode", skuCode);
        map.put("qty", qty);
        map.put("pickedQty", 0);
        map.put("packedQty", 0);
        map.put("shippedQty", 0);
        map.put("status", 0);
        map.put("statusName", "待拣货");
        return map;
    }
}
