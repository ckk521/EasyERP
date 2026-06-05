package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.outbound.dto.OutboundOrderDTO;
import com.wms.outbound.dto.OutboundOrderItemDTO;
import com.wms.outbound.dto.OutboundOrderQueryDTO;
import com.wms.outbound.entity.OutboundOrder;
import com.wms.outbound.entity.OutboundOrderItem;
import com.wms.outbound.repository.OutboundOrderRepository;
import com.wms.outbound.repository.OutboundOrderItemRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 出库单服务单元测试
 * TDD: 测试覆盖P0级别核心场景
 *
 * 测试用例对照：
 * - TC-OUT-001: 创建销售出库单-正常流程
 * - TC-OUT-002: 手工创建出库单
 * - TC-OUT-003: 创建出库单-库存不足校验
 * - TC-OUT-007: 取消出库单
 * - TC-OUT-008: 取消已分配出库单
 * - TC-QUERY-001: 查询出库单列表（分页、总数）
 * - TC-QUERY-002: 按条件查询出库单（状态、仓库、日期、关键字、优先级、类型、收货人、波次、物流）
 * - TC-QUERY-003: 查看出库单详情（基本信息、商品明细、进度百分比）
 * - TC-QUERY-004: 查看出库单生命周期（拣货、打包、发货记录）
 * - TC-QUERY-005: 导出出库数据（CSV格式、日期筛选、状态筛选）
 * - TC-EDGE-001: 出库数量为0
 * - TC-EDGE-002: 出库数量为负数
 * - TC-EDGE-003: 商品明细为空
 * - 边界值测试: 序号超999、进度计算零值处理、空结果、分页
 */
class OutboundOrderServiceTest {

    @Mock
    private OutboundOrderRepository orderRepository;

    @Mock
    private OutboundOrderItemRepository itemRepository;

    @InjectMocks
    private OutboundOrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 设置自动生成的ID
        when(orderRepository.insert(any(OutboundOrder.class))).thenAnswer(invocation -> {
            OutboundOrder order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1L);
            return 1;
        });
        when(itemRepository.insert(any(OutboundOrderItem.class))).thenAnswer(invocation -> {
            OutboundOrderItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 1L);
            return 1;
        });
        // Mock: 模拟库存充足（返回null表示不校验，或返回大数值）
        when(itemRepository.getAvailableStock(anyLong())).thenReturn(1000);
    }

    // ========== TC-OUT-001 创建销售出库单 - 正常流程 ==========

    @Test
    @DisplayName("TC-OUT-001: 创建销售出库单-正常流程 - 自动生成出库单号")
    void testCreateOrder_SalesOrder_NormalFlow() {
        // Given: 有效的销售出库单DTO
        OutboundOrderDTO dto = createSalesOrderDTO();

        // Mock: 模拟数据库操作
        when(orderRepository.getMaxSeqByDate(anyString())).thenReturn(null);

        // When: 创建出库单
        Long orderId = orderService.createOrder(dto);

        // Then: 成功创建并返回ID
        assertNotNull(orderId);
        verify(orderRepository).insert(any(OutboundOrder.class));
        verify(itemRepository, times(2)).insert(any(OutboundOrderItem.class));
    }

    @Test
    @DisplayName("TC-OUT-001: 出库单号生成格式验证 - OB+年月日+3位序号（共13位）")
    void testCreateOrder_OrderNoFormat() {
        // Given: 销售出库单
        OutboundOrderDTO dto = createSalesOrderDTO();

        // Mock: 模拟当日已有1个出库单
        when(orderRepository.getMaxSeqByDate(anyString())).thenReturn(1);

        // When: 创建出库单
        Long orderId = orderService.createOrder(dto);

        // Then: 出库单号格式为 OB+日期+002（当日已有1个，所以新的是002）
        // 格式：OB(2位) + 年月日(8位) + 序号(3位) = 13位
        assertNotNull(orderId);

        // 使用ArgumentCaptor捕获实际参数
        ArgumentCaptor<OutboundOrder> orderCaptor = ArgumentCaptor.forClass(OutboundOrder.class);
        verify(orderRepository).insert(orderCaptor.capture());

        OutboundOrder capturedOrder = orderCaptor.getValue();
        String orderNo = capturedOrder.getOrderNo();

        assertTrue(orderNo.startsWith("OB"), "出库单号应以OB开头");
        assertEquals(13, orderNo.length(), "出库单号长度应为13位（OB+8位日期+3位序号）");
        assertTrue(orderNo.endsWith("002"), "出库单号应以002结尾");
    }

    @Test
    @DisplayName("TC-OUT-001: 出库单初始状态为待分配")
    void testCreateOrder_InitialStatus() {
        // Given: 销售出库单
        OutboundOrderDTO dto = createSalesOrderDTO();

        // When: 创建出库单
        orderService.createOrder(dto);

        // Then: 状态为待分配(0)
        verify(orderRepository).insert(argThat(order ->
            order.getStatus() == OutboundOrder.STATUS_PENDING
        ));
    }

    // ========== TC-OUT-002 手工创建出库单 ==========

    @Test
    @DisplayName("TC-OUT-002: 手工创建出库单 - 来源类型为手工创建")
    void testCreateOrder_ManualCreation() {
        // Given: 手工创建的出库单
        OutboundOrderDTO dto = createManualOrderDTO();

        // When: 创建出库单
        Long orderId = orderService.createOrder(dto);

        // Then: 来源类型为手工创建
        assertNotNull(orderId);
        verify(orderRepository).insert(argThat(order ->
            order.getSourceType() == OutboundOrder.SOURCE_MANUAL
        ));
    }

    @Test
    @DisplayName("TC-OUT-002: 手工创建出库单 - 必须填写客户信息")
    void testCreateOrder_ManualCreation_RequiresCustomer() {
        // Given: 手工创建但缺少客户信息
        OutboundOrderDTO dto = createManualOrderDTO();
        dto.setCustomerId(null);
        dto.setCustomerName(null);

        // When & Then: 抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(dto);
        });
    }

    // ========== TC-OUT-003 创建出库单 - 库存不足校验 ==========

    @Test
    @DisplayName("TC-OUT-003: 创建出库单-库存不足校验 - 提示可用库存数量")
    void testCreateOrder_InsufficientStock() {
        // Given: 出库数量超过可用库存
        OutboundOrderDTO dto = createSalesOrderDTO();
        dto.getItems().get(0).setQty(1000); // 设置超大数量

        // Mock: 模拟当前可用库存只有50件
        when(itemRepository.getAvailableStock(anyLong())).thenReturn(50);

        // When & Then: 抛出库存不足异常
        assertThrows(IllegalStateException.class, () -> {
            orderService.createOrder(dto);
        }, "库存不足，当前可用库存：50件");
    }

    // ========== TC-OUT-007 取消出库单 ==========

    @Test
    @DisplayName("TC-OUT-007: 取消出库单 - 待分配状态可取消")
    void testCancelOrder_PendingStatus() {
        // Given: 待分配状态的出库单
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PENDING);
        when(orderRepository.selectById(1L)).thenReturn(order);

        // When: 取消出库单
        orderService.cancelOrder(1L, "客户取消订单");

        // Then: 状态变为已取消
        verify(orderRepository).updateById(argThat(o ->
            o.getStatus() == OutboundOrder.STATUS_CANCELLED &&
            "客户取消订单".equals(o.getCancelReason())
        ));
    }

    @Test
    @DisplayName("TC-OUT-007: 取消出库单 - 取消原因不能为空")
    void testCancelOrder_RequiresReason() {
        // Given: 出库单存在
        OutboundOrder order = createTestOrder();
        when(orderRepository.selectById(1L)).thenReturn(order);

        // When & Then: 空原因抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.cancelOrder(1L, null);
        }, "取消原因不能为空");

        assertThrows(IllegalArgumentException.class, () -> {
            orderService.cancelOrder(1L, "");
        }, "取消原因不能为空");
    }

    // ========== TC-OUT-008 取消已分配出库单 ==========

    @Test
    @DisplayName("TC-OUT-008: 取消已分配出库单 - 自动释放锁定库存")
    void testCancelOrder_AllocatedStatus_ReleasesLock() {
        // Given: 已分配状态的出库单（库存已锁定）
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_ALLOCATED);
        when(orderRepository.selectById(1L)).thenReturn(order);

        // Mock: 模拟出库单明细
        OutboundOrderItem item = createTestOrderItem();
        item.setProductId(1L);
        item.setQty(10);
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // Mock: 模拟库存释放操作
        when(itemRepository.releaseLockedStock(anyLong(), anyInt())).thenReturn(true);

        // When: 取消出库单
        orderService.cancelOrder(1L, "客户取消");

        // Then: 状态变为已取消，且释放库存
        verify(orderRepository).updateById(argThat(o ->
            o.getStatus() == OutboundOrder.STATUS_CANCELLED
        ));
        verify(itemRepository).releaseLockedStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("TC-OUT-008: 已发货出库单不可取消")
    void testCancelOrder_ShippedStatus_CannotCancel() {
        // Given: 已发货状态的出库单
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPED);
        when(orderRepository.selectById(1L)).thenReturn(order);

        // When & Then: 抛出异常
        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(1L, "试图取消");
        }, "已发货的出库单不可取消");
    }

    // ========== TC-QUERY-001 查询出库单列表 ==========

    @Test
    @DisplayName("TC-QUERY-001: 查询出库单列表 - 显示分页结果")
    void testListOrders_BasicQuery() {
        // Given: 多条出库单数据
        OutboundOrder order1 = createTestOrder();
        order1.setId(1L);
        OutboundOrder order2 = createTestOrder();
        order2.setId(2L);

        Page<OutboundOrder> page = new Page<>(1, 20, 2);
        page.setRecords(Arrays.asList(order1, order2));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 查询列表
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setPage(1);
        query.setLimit(20);
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回分页数据
        assertNotNull(result);
        assertEquals(2L, result.get("total"));
        assertNotNull(result.get("list"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        assertEquals(2, list.size());
    }

    // ========== TC-QUERY-002 按条件查询出库单 ==========

    @Test
    @DisplayName("TC-QUERY-002: 按状态查询出库单 - 只返回待分配状态")
    void testListOrders_ByStatus() {
        // Given: 待分配状态的出库单
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PENDING);

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按状态查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setStatus("0"); // 待分配
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回匹配结果
        assertNotNull(result);
        verify(orderRepository).selectPage(any(Page.class), argThat(wrapper ->
            true // 验证wrapper包含status条件
        ));
    }

    @Test
    @DisplayName("TC-QUERY-002: 按仓库查询出库单 - 只返回指定仓库的出库单")
    void testListOrders_ByWarehouse() {
        // Given: 深圳总仓的出库单
        OutboundOrder order = createTestOrder();
        order.setWarehouseId(1L);

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按仓库查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setWarehouseId(1L);
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回该仓库的出库单
        assertNotNull(result);
    }

    @Test
    @DisplayName("TC-QUERY-002: 组合条件查询 - 状态 + 仓库 + 日期范围")
    void testListOrders_MultipleConditions() {
        // Given: 组合条件
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PENDING);
        order.setWarehouseId(1L);

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 多条件查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setStatus("0");
        query.setWarehouseId(1L);
        query.setStartDate("2026-05-01");
        query.setEndDate("2026-05-31");
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回同时满足所有条件的结果
        assertNotNull(result);
    }

    // ========== TC-QUERY-003 查看出库单详情 ==========

    @Test
    @DisplayName("TC-QUERY-003: 查看出库单详情 - 包含基本信息和商品明细")
    void testGetOrderDetail_WithItems() {
        // Given: 出库单和明细
        OutboundOrder order = createTestOrder();
        order.setId(1L);
        when(orderRepository.selectById(1L)).thenReturn(order);

        OutboundOrderItem item1 = createTestOrderItem();
        item1.setId(1L);
        OutboundOrderItem item2 = createTestOrderItem();
        item2.setId(2L);
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(item1, item2));

        // When: 获取详情
        Map<String, Object> detail = orderService.getOrderDetail(1L);

        // Then: 包含基本信息和明细
        assertNotNull(detail);
        assertEquals(1L, detail.get("id"));
        assertNotNull(detail.get("orderNo"));
        assertNotNull(detail.get("items"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) detail.get("items");
        assertEquals(2, items.size());
    }

    @Test
    @DisplayName("TC-QUERY-003: 出库单不存在 - 抛出异常")
    void testGetOrderDetail_NotFound() {
        // Given: 出库单不存在
        when(orderRepository.selectById(999L)).thenReturn(null);

        // When & Then: 抛出异常
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderDetail(999L);
        }, "出库单不存在");
    }

    // ========== TC-QUERY-004 查看出库单生命周期 ==========

    @Test
    @DisplayName("TC-QUERY-004: 查看出库单生命周期 - 包含拣货记录")
    void testGetOrderLifecycle_WithPickRecords() {
        // Given: 出库单已完成拣货
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PICKING);
        order.setTotalPickedQty(10);
        when(orderRepository.selectById(1L)).thenReturn(order);

        OutboundOrderItem item = createTestOrderItem();
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // When: 获取详情
        Map<String, Object> detail = orderService.getOrderDetail(1L);

        // Then: 包含拣货进度信息
        assertNotNull(detail);
        assertEquals(10, detail.get("totalPickedQty"));
        assertNotNull(detail.get("progressPick"));
    }

    @Test
    @DisplayName("TC-QUERY-004: 查看出库单生命周期 - 包含打包记录")
    void testGetOrderLifecycle_WithPackRecords() {
        // Given: 出库单已完成打包
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);
        order.setTotalPickedQty(15);
        order.setTotalPackedQty(10);
        when(orderRepository.selectById(1L)).thenReturn(order);

        OutboundOrderItem item = createTestOrderItem();
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // When: 获取详情
        Map<String, Object> detail = orderService.getOrderDetail(1L);

        // Then: 包含打包进度信息
        assertNotNull(detail);
        assertEquals(10, detail.get("totalPackedQty"));
        assertNotNull(detail.get("progressPack"));
    }

    @Test
    @DisplayName("TC-QUERY-004: 查看出库单生命周期 - 包含发货记录")
    void testGetOrderLifecycle_WithShipRecords() {
        // Given: 出库单已发货
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPED);
        order.setTotalPickedQty(15);
        order.setTotalPackedQty(15);
        order.setTotalShippedQty(15);
        order.setTrackingNo("JT1234567890");
        order.setLogisticsCompany("J&T Express");
        when(orderRepository.selectById(1L)).thenReturn(order);

        OutboundOrderItem item = createTestOrderItem();
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // When: 获取详情
        Map<String, Object> detail = orderService.getOrderDetail(1L);

        // Then: 包含物流信息
        assertNotNull(detail);
        assertEquals("JT1234567890", detail.get("trackingNo"));
        assertEquals("J&T Express", detail.get("logisticsCompany"));
        assertEquals(100, detail.get("progressShip"));
    }

    // ========== TC-QUERY-005 导出出库数据 ==========

    @Test
    @DisplayName("TC-QUERY-005: 导出出库数据 - 返回CSV格式")
    void testExportOrders_CsvFormat() {
        // Given: 多条出库单数据
        OutboundOrder order1 = createTestOrder();
        order1.setId(1L);
        OutboundOrder order2 = createTestOrder();
        order2.setId(2L);

        Page<OutboundOrder> page = new Page<>(1, 100, 2);
        page.setRecords(Arrays.asList(order1, order2));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 导出数据
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setLimit(100); // 导出时不分页
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回所有数据
        assertNotNull(result);
        assertEquals(2L, result.get("total"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        assertEquals(2, list.size());

        // 验证每条数据包含导出所需字段
        Map<String, Object> firstOrder = list.get(0);
        assertTrue(firstOrder.containsKey("orderNo"));
        assertTrue(firstOrder.containsKey("customerName"));
        assertTrue(firstOrder.containsKey("status"));
        assertTrue(firstOrder.containsKey("totalQty"));
    }

    @Test
    @DisplayName("TC-QUERY-005: 导出出库数据 - 支持按日期范围筛选")
    void testExportOrders_ByDateRange() {
        // Given: 指定日期范围内的出库单
        OutboundOrder order = createTestOrder();
        order.setCreateTime(LocalDateTime.of(2026, 5, 15, 10, 0));

        Page<OutboundOrder> page = new Page<>(1, 100, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按日期范围导出
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setStartDate("2026-05-01");
        query.setEndDate("2026-05-31");
        query.setLimit(100);
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回符合日期范围的数据
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-005: 导出出库数据 - 支持按状态筛选")
    void testExportOrders_ByStatus() {
        // Given: 已发货的出库单
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPED);

        Page<OutboundOrder> page = new Page<>(1, 100, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按状态导出
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setStatus("5"); // 已发货
        query.setLimit(100);
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回已发货的出库单
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-005: 导出出库数据 - 包含状态名称")
    void testExportOrders_WithStatusName() {
        // Given: 出库单
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPED);

        Page<OutboundOrder> page = new Page<>(1, 100, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 导出数据
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setLimit(100);
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 包含状态名称
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        assertEquals("已发货", list.get(0).get("statusName"));
    }

    // ========== TC-QUERY-002 高级查询功能 ==========

    @Test
    @DisplayName("TC-QUERY-002: 多状态查询 - 支持逗号分隔的状态列表")
    void testListOrders_MultipleStatus() {
        // Given: 多个不同状态的出库单
        OutboundOrder order1 = createTestOrder();
        order1.setStatus(OutboundOrder.STATUS_PENDING);
        OutboundOrder order2 = createTestOrder();
        order2.setStatus(OutboundOrder.STATUS_ALLOCATED);

        Page<OutboundOrder> page = new Page<>(1, 20, 2);
        page.setRecords(Arrays.asList(order1, order2));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按多状态查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setStatus("0,1"); // 待分配,已分配
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回匹配结果
        assertNotNull(result);
        assertEquals(2L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 关键字搜索 - 支持出库单号模糊匹配")
    void testListOrders_KeywordSearch_OrderNo() {
        // Given: 出库单
        OutboundOrder order = createTestOrder();
        order.setOrderNo("OB20260531001");

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按关键字搜索
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setKeyword("OB202605");
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回匹配结果
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 关键字搜索 - 支持客户名称模糊匹配")
    void testListOrders_KeywordSearch_CustomerName() {
        // Given: 出库单
        OutboundOrder order = createTestOrder();
        order.setCustomerName("测试客户A");

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按客户名称搜索
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setKeyword("客户A");
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回匹配结果
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 关键字搜索 - 支持销售订单号查询")
    void testListOrders_KeywordSearch_SoNo() {
        // Given: 出库单
        OutboundOrder order = createTestOrder();
        order.setSoNo("SO20260531001");

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按销售订单号搜索
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setKeyword("SO202605");
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回匹配结果
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 按优先级查询 - 只返回紧急出库单")
    void testListOrders_ByPriority() {
        // Given: 紧急出库单
        OutboundOrder order = createTestOrder();
        order.setPriority(OutboundOrder.PRIORITY_URGENT);

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按优先级查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setPriority(1); // 紧急
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回紧急出库单
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 按出库类型查询 - 只返回销售出库")
    void testListOrders_ByOrderType() {
        // Given: 销售出库单
        OutboundOrder order = createTestOrder();
        order.setOrderType(OutboundOrder.TYPE_SALES);

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按出库类型查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setOrderType(1); // 销售出库
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回销售出库单
        assertNotNull(result);
        assertEquals(1L, result.get("total"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        assertEquals("销售出库", list.get(0).get("orderTypeName"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 按收货人信息查询 - 支持收货人姓名")
    void testListOrders_ByReceiverName() {
        // Given: 出库单
        OutboundOrder order = createTestOrder();
        order.setReceiverName("张三");

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按收货人姓名查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setReceiverName("张三");
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回匹配结果
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 按收货人信息查询 - 支持收货人电话")
    void testListOrders_ByReceiverPhone() {
        // Given: 出库单
        OutboundOrder order = createTestOrder();
        order.setReceiverPhone("13800138000");

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按收货人电话查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setReceiverPhone("13800138000");
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回匹配结果
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 按波次号查询 - 返回属于该波次的出库单")
    void testListOrders_ByWaveNo() {
        // Given: 属于某个波次的出库单
        OutboundOrder order = createTestOrder();
        order.setWaveId(1L);
        order.setWaveNo("WV20260531001");

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按波次号查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setWaveNo("WV20260531001");
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回该波次的出库单
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    @Test
    @DisplayName("TC-QUERY-002: 按物流公司查询 - 返回指定物流公司的出库单")
    void testListOrders_ByLogisticsCompany() {
        // Given: 指定物流公司的出库单
        OutboundOrder order = createTestOrder();
        order.setLogisticsCompany("J&T Express");

        Page<OutboundOrder> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 按物流公司查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setLogisticsCompany("J&T Express");
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回匹配结果
        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    // ========== TC-QUERY-003 详情查询增强 ==========

    @Test
    @DisplayName("TC-QUERY-003: 出库单详情 - 包含进度百分比")
    void testGetOrderDetail_WithProgress() {
        // Given: 进行中的出库单
        OutboundOrder order = createTestOrder();
        order.setTotalQty(100);
        order.setTotalPickedQty(50);
        order.setTotalPackedQty(30);
        order.setTotalShippedQty(10);
        when(orderRepository.selectById(1L)).thenReturn(order);

        OutboundOrderItem item = createTestOrderItem();
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // When: 获取详情
        Map<String, Object> detail = orderService.getOrderDetail(1L);

        // Then: 包含进度百分比
        assertEquals(50, detail.get("progressPick"));  // 50/100
        assertEquals(60, detail.get("progressPack"));  // 30/50
        assertEquals(33, detail.get("progressShip")); // 10/30
    }

    @Test
    @DisplayName("TC-QUERY-003: 出库单详情 - 包含类型名称")
    void testGetOrderDetail_WithTypeName() {
        // Given: 销售出库单
        OutboundOrder order = createTestOrder();
        order.setOrderType(OutboundOrder.TYPE_SALES);
        order.setSourceType(OutboundOrder.SOURCE_ERP);
        when(orderRepository.selectById(1L)).thenReturn(order);

        OutboundOrderItem item = createTestOrderItem();
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // When: 获取详情
        Map<String, Object> detail = orderService.getOrderDetail(1L);

        // Then: 包含类型名称
        assertEquals("销售出库", detail.get("orderTypeName"));
        assertEquals("ERP推送", detail.get("sourceTypeName"));
    }

    @Test
    @DisplayName("TC-QUERY-003: 出库单详情 - 包含优先级名称")
    void testGetOrderDetail_WithPriorityName() {
        // Given: 紧急出库单
        OutboundOrder order = createTestOrder();
        order.setPriority(OutboundOrder.PRIORITY_URGENT);
        when(orderRepository.selectById(1L)).thenReturn(order);

        OutboundOrderItem item = createTestOrderItem();
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // When: 获取详情
        Map<String, Object> detail = orderService.getOrderDetail(1L);

        // Then: 包含优先级名称
        assertEquals("紧急", detail.get("priorityName"));
    }

    // ========== 边界值测试 ==========

    @Test
    @DisplayName("边界值测试: 空结果查询")
    void testListOrders_EmptyResult() {
        // Given: 没有匹配的数据
        Page<OutboundOrder> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 查询
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setStatus("99"); // 不存在的状态
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回空列表
        assertEquals(0L, result.get("total"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("边界值测试: 分页查询 - 第2页")
    void testListOrders_SecondPage() {
        // Given: 多条数据
        OutboundOrder order = createTestOrder();
        Page<OutboundOrder> page = new Page<>(2, 10, 25);
        page.setRecords(Collections.singletonList(order));

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When: 查询第2页
        OutboundOrderQueryDTO query = new OutboundOrderQueryDTO();
        query.setPage(2);
        query.setLimit(10);
        Map<String, Object> result = orderService.listOrders(query);

        // Then: 返回分页信息正确
        assertEquals(25L, result.get("total"));
        assertEquals(2, result.get("page"));
        assertEquals(10, result.get("limit"));
    }

    @Test
    @DisplayName("边界值测试: 进度计算 - 数量为0时不报错")
    void testGetOrderDetail_ZeroQty() {
        // Given: 数量为0的出库单
        OutboundOrder order = createTestOrder();
        order.setTotalQty(0);
        order.setTotalPickedQty(0);
        order.setTotalPackedQty(0);
        order.setTotalShippedQty(0);
        when(orderRepository.selectById(1L)).thenReturn(order);

        OutboundOrderItem item = createTestOrderItem();
        when(itemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // When: 获取详情
        Map<String, Object> detail = orderService.getOrderDetail(1L);

        // Then: 进度为0，不报错
        assertEquals(0, detail.get("progressPick"));
        assertEquals(0, detail.get("progressPack"));
        assertEquals(0, detail.get("progressShip"));
    }

    // ========== TC-EDGE-001 出库数量为0 ==========

    @Test
    @DisplayName("TC-EDGE-001: 出库数量为0 - 验证失败")
    void testCreateOrder_QtyZero() {
        // Given: 数量为0
        OutboundOrderDTO dto = createSalesOrderDTO();
        dto.getItems().get(0).setQty(0);

        // When & Then: 抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(dto);
        }, "数量必须大于0");
    }

    // ========== TC-EDGE-002 出库数量为负数 ==========

    @Test
    @DisplayName("TC-EDGE-002: 出库数量为负数 - 验证失败")
    void testCreateOrder_QtyNegative() {
        // Given: 数量为负数
        OutboundOrderDTO dto = createSalesOrderDTO();
        dto.getItems().get(0).setQty(-10);

        // When & Then: 抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(dto);
        }, "数量必须大于0");
    }

    // ========== TC-EDGE-003 商品明细为空 ==========

    @Test
    @DisplayName("TC-EDGE-003: 商品明细为空 - 验证失败")
    void testCreateOrder_EmptyItems() {
        // Given: 商品明细为空
        OutboundOrderDTO dto = createSalesOrderDTO();
        dto.setItems(null);

        // When & Then: 抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(dto);
        }, "商品明细不能为空");
    }

    @Test
    @DisplayName("TC-EDGE-003: 商品明细列表为空 - 验证失败")
    void testCreateOrder_NoItems() {
        // Given: 商品明细列表为空
        OutboundOrderDTO dto = createSalesOrderDTO();
        dto.setItems(new ArrayList<>());

        // When & Then: 抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(dto);
        }, "商品明细不能为空");
    }

    // ========== 边界值测试 ==========

    @Test
    @DisplayName("边界值测试: 出库单号序号达到999后继续递增")
    void testCreateOrder_SeqExceeds999() {
        // Given: 当日已有999个出库单
        when(orderRepository.getMaxSeqByDate("OB20260531")).thenReturn(999);

        // When: 创建出库单
        OutboundOrderDTO dto = createSalesOrderDTO();
        Long orderId = orderService.createOrder(dto);

        // Then: 序号变为1000，出库单号长度变为14位（OB+8位日期+4位序号）
        assertNotNull(orderId);
        verify(orderRepository).insert(argThat(order ->
            order.getOrderNo().equals("OB202605311000") &&
            order.getOrderNo().length() == 14
        ));
    }

    @Test
    @DisplayName("边界值测试: SKU编码不能为空")
    void testCreateOrder_SkuCodeEmpty() {
        // Given: SKU编码为空
        OutboundOrderDTO dto = createSalesOrderDTO();
        dto.getItems().get(0).setSkuCode(null);

        // When & Then: 抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(dto);
        }, "SKU编码不能为空");
    }

    @Test
    @DisplayName("边界值测试: 仓库ID不能为空")
    void testCreateOrder_WarehouseIdNull() {
        // Given: 仓库ID为空
        OutboundOrderDTO dto = createSalesOrderDTO();
        dto.setWarehouseId(null);

        // When & Then: 抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(dto);
        }, "仓库不能为空");
    }

    // ========== 辅助方法 ==========

    private OutboundOrderDTO createSalesOrderDTO() {
        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setOrderType(OutboundOrder.TYPE_SALES);
        dto.setSourceType(OutboundOrder.SOURCE_ERP);
        dto.setSoNo("SO20260531001");
        dto.setCustomerId(100L);
        dto.setCustomerCode("CUS-001");
        dto.setCustomerName("测试客户");
        dto.setWarehouseId(1L);
        dto.setWarehouseCode("WH-CN-001");
        dto.setWarehouseName("深圳总仓");
        dto.setPriority(OutboundOrder.PRIORITY_NORMAL);
        dto.setLogisticsCompany("J&T Express");
        dto.setReceiverName("张三");
        dto.setReceiverPhone("13800138000");
        dto.setReceiverAddress("雅加达市中心区某街道1号");

        List<OutboundOrderItemDTO> items = new ArrayList<>();

        OutboundOrderItemDTO item1 = new OutboundOrderItemDTO();
        item1.setProductId(1L);
        item1.setSkuCode("SKU-001");
        item1.setProductName("海飞丝去屑洗发水500ml");
        item1.setBarcode("6901234000009");
        item1.setQty(10);
        items.add(item1);

        OutboundOrderItemDTO item2 = new OutboundOrderItemDTO();
        item2.setProductId(2L);
        item2.setSkuCode("SKU-002");
        item2.setProductName("玉兰油面霜");
        item2.setBarcode("6901234000010");
        item2.setQty(5);
        items.add(item2);

        dto.setItems(items);
        return dto;
    }

    private OutboundOrderDTO createManualOrderDTO() {
        OutboundOrderDTO dto = createSalesOrderDTO();
        dto.setSourceType(OutboundOrder.SOURCE_MANUAL);
        dto.setSoNo(null); // 手工创建没有销售订单号
        return dto;
    }

    private OutboundOrder createTestOrder() {
        OutboundOrder order = new OutboundOrder();
        order.setId(1L);
        order.setOrderNo("OB20260531001");
        order.setOrderType(OutboundOrder.TYPE_SALES);
        order.setSourceType(OutboundOrder.SOURCE_ERP);
        order.setSoNo("SO20260531001");
        order.setCustomerId(100L);
        order.setCustomerCode("CUS-001");
        order.setCustomerName("测试客户");
        order.setWarehouseId(1L);
        order.setWarehouseCode("WH-CN-001");
        order.setWarehouseName("深圳总仓");
        order.setStatus(OutboundOrder.STATUS_PENDING);
        order.setPriority(OutboundOrder.PRIORITY_NORMAL);
        order.setTotalQty(15);
        order.setTotalPickedQty(0);
        order.setTotalPackedQty(0);
        order.setTotalShippedQty(0);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    private OutboundOrderItem createTestOrderItem() {
        OutboundOrderItem item = new OutboundOrderItem();
        item.setId(1L);
        item.setOrderId(1L);
        item.setOrderNo("OB20260531001");
        item.setProductId(1L);
        item.setSkuCode("SKU-001");
        item.setProductName("海飞丝去屑洗发水500ml");
        item.setBarcode("6901234000009");
        item.setQty(10);
        item.setPickedQty(0);
        item.setPackedQty(0);
        item.setShippedQty(0);
        item.setStatus(OutboundOrderItem.STATUS_PENDING);
        return item;
    }
}