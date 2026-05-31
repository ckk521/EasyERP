package com.wms.stocktake.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.stocktake.dto.*;
import com.wms.stocktake.entity.StocktakeOrder;
import com.wms.stocktake.service.StocktakeService;
import com.wms.system.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 盘点控制器单元测试
 * 使用MockMvc独立测试Controller层，不加载Spring上下文
 */
@ExtendWith(MockitoExtension.class)
class StocktakeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StocktakeService stocktakeService;

    @InjectMocks
    private StocktakeController stocktakeController;

    private ObjectMapper objectMapper;

    private String testToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(stocktakeController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // 支持Java 8日期类型

        // 生成有效的测试JWT token
        testToken = JwtUtil.generateToken(1L, "admin");
    }

    // ========== TC-STK-001 API测试: 创建盘点单 ==========

    @Test
    @DisplayName("TC-STK-001 API: 创建全盘盘点单 - 成功返回200")
    void testCreateOrder_Success() throws Exception {
        // Given
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setWarehouseId(1L);
        dto.setStocktakeType(StocktakeOrder.TYPE_FULL);
        dto.setBlindMode(StocktakeOrder.BLIND_MODE_OFF);
        dto.setScopeType("all");
        dto.setPlanDate(LocalDate.now());

        when(stocktakeService.createOrder(any(StocktakeCreateDTO.class), anyLong(), anyString()))
                .thenReturn(1L);

        // When & Then
        mockMvc.perform(post("/api/stocktake/create")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("盘点单创建成功"))
                .andExpect(jsonPath("$.data.orderId").value(1));
    }

    @Test
    @DisplayName("TC-STK-007 API: 创建盘点单 - 参数校验失败返回400")
    void testCreateOrder_ValidationError() throws Exception {
        // Given: 缺少必填参数
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        // 不设置warehouseId

        when(stocktakeService.createOrder(any(StocktakeCreateDTO.class), anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("仓库不能为空"));

        // When & Then
        mockMvc.perform(post("/api/stocktake/create")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("仓库不能为空"));
    }

    // ========== TC-STK-011 API测试: 盘点作业 ==========

    @Test
    @DisplayName("TC-STK-011 API: 盘点作业 - 成功")
    void testCountItem_Success() throws Exception {
        // Given
        StocktakeCountDTO dto = new StocktakeCountDTO();
        dto.setItemId(1L);
        dto.setCountedQty(100);

        doNothing().when(stocktakeService).countItem(any(StocktakeCountDTO.class), anyLong(), anyString());

        // When & Then
        mockMvc.perform(post("/api/stocktake/count")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("盘点录入成功"));
    }

    @Test
    @DisplayName("TC-STK-011 API: 盘点作业 - 盘点明细不存在返回400")
    void testCountItem_ItemNotFound() throws Exception {
        // Given
        StocktakeCountDTO dto = new StocktakeCountDTO();
        dto.setItemId(999L);
        dto.setCountedQty(100);

        doThrow(new IllegalArgumentException("盘点明细不存在"))
                .when(stocktakeService).countItem(any(StocktakeCountDTO.class), anyLong(), anyString());

        // When & Then
        mockMvc.perform(post("/api/stocktake/count")
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ========== TC-STK-020 API测试: 完成盘点 ==========

    @Test
    @DisplayName("TC-STK-020 API: 完成盘点 - 成功")
    void testFinishOrder_Success() throws Exception {
        // Given
        doNothing().when(stocktakeService).finishOrder(1L);

        // When & Then
        mockMvc.perform(post("/api/stocktake/finish/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("盘点已完成，进入审核状态"));
    }

    @Test
    @DisplayName("TC-STK-020 API: 完成盘点 - 未全部盘点返回400")
    void testFinishOrder_NotAllCounted() throws Exception {
        // Given
        doThrow(new IllegalStateException("还有未盘点的商品"))
                .when(stocktakeService).finishOrder(1L);

        // When & Then
        mockMvc.perform(post("/api/stocktake/finish/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("还有未盘点的商品"));
    }

    // ========== TC-STK-031 API测试: 查询盘点单列表 ==========

    @Test
    @DisplayName("TC-STK-031 API: 查询盘点单列表 - 成功返回分页数据")
    void testQueryOrders_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("list", Arrays.asList(createTestOrderMap(1L), createTestOrderMap(2L)));
        result.put("total", 2L);

        when(stocktakeService.queryOrders(any(StocktakeQueryDTO.class))).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/stocktake/list")
                .param("page", "1")
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("TC-STK-031 API: 查询盘点单列表 - 按状态筛选")
    void testQueryOrders_ByStatus() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("list", Collections.singletonList(createTestOrderMap(1L)));
        result.put("total", 1L);

        when(stocktakeService.queryOrders(any(StocktakeQueryDTO.class))).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/stocktake/list")
                .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    // ========== API测试: 获取盘点单详情 ==========

    @Test
    @DisplayName("API: 获取盘点单详情 - 成功")
    void testGetOrderDetail_Success() throws Exception {
        // Given
        Map<String, Object> result = createTestOrderMap(1L);
        result.put("items", Collections.emptyList());

        when(stocktakeService.getOrderDetail(1L)).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/stocktake/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("API: 获取盘点单详情 - 不存在返回400")
    void testGetOrderDetail_NotFound() throws Exception {
        // Given
        when(stocktakeService.getOrderDetail(999L))
                .thenThrow(new IllegalArgumentException("盘点单不存在"));

        // When & Then
        mockMvc.perform(get("/api/stocktake/detail/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ========== TC-STK-035 API测试: 取消盘点单 ==========

    @Test
    @DisplayName("TC-STK-035 API: 取消盘点单 - 成功")
    void testCancelOrder_Success() throws Exception {
        // Given
        doNothing().when(stocktakeService).cancelOrder(eq(1L), anyString());

        // When & Then
        mockMvc.perform(post("/api/stocktake/cancel/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"测试取消\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("盘点单已取消"));
    }

    @Test
    @DisplayName("TC-STK-036 API: 取消盘点单 - 盘点中状态返回400")
    void testCancelOrder_CountingStatus() throws Exception {
        // Given
        doThrow(new IllegalStateException("只有待盘点状态的盘点单可以取消"))
                .when(stocktakeService).cancelOrder(eq(1L), anyString());

        // When & Then
        mockMvc.perform(post("/api/stocktake/cancel/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"测试取消\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> createTestOrderMap(Long id) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("orderNo", "ST20260527" + String.format("%04d", id));
        map.put("warehouseId", 1L);
        map.put("warehouseCode", "WH-CN-001");
        map.put("warehouseName", "深圳总仓");
        map.put("stocktakeType", 1);
        map.put("stocktakeTypeName", "全盘");
        map.put("blindMode", 0);
        map.put("status", 0);
        map.put("statusName", "待盘点");
        map.put("totalItems", 156);
        map.put("countedItems", 0);
        map.put("diffItems", 0);
        map.put("accuracyRate", null);
        map.put("planDate", LocalDate.now());
        map.put("startTime", null);
        map.put("finishTime", null);
        map.put("remark", null);
        map.put("createUserName", "admin");
        map.put("createTime", LocalDateTime.now());
        return map;
    }
}
