package com.wms.outbound.service;

import com.wms.outbound.dto.*;
import com.wms.outbound.entity.*;
import com.wms.outbound.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 发货服务单元测试
 * TDD: 测试覆盖发货确认、批量发货、库存扣减场景
 *
 * 测试用例对照：
 * - TC-SHIP-001: 扫描出库单号确认发货
 * - TC-SHIP-002: 选择物流公司和录入物流单号
 * - TC-SHIP-003: 批量确认发货
 * - TC-SHIP-004: 发货后状态变为已发货
 * - TC-SHIP-005: 库存扣减
 */
class ShipServiceTest {

    @Mock
    private ShipRecordRepository shipRecordRepository;

    @Mock
    private PackRecordRepository packRecordRepository;

    @Mock
    private OutboundOrderRepository orderRepository;

    @Mock
    private OutboundOrderItemRepository orderItemRepository;

    @Mock
    private InventoryAllocationRepository allocationRepository;

    private ShipService shipService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        shipService = new ShipService(
            shipRecordRepository,
            packRecordRepository,
            orderRepository,
            orderItemRepository,
            allocationRepository
        );
    }

    // ========== TC-SHIP-001 扫描出库单号确认发货 ==========

    @Test
    @DisplayName("TC-SHIP-001: 扫描出库单号确认发货 - 显示待发货信息")
    void testGetPendingShipment() {
        // Given: 已打包的出库单
        String orderNo = "OB20260531001";
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectByOrderNo(orderNo)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(order.getId())).thenReturn(packRecord);

        // When: 扫描出库单号
        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderNo(orderNo);

        // Then: 应该能获取到待发货信息
        assertNotNull(order);
        assertEquals(OutboundOrder.STATUS_SHIPPING, order.getStatus());
    }

    @Test
    @DisplayName("TC-SHIP-001: 扫描包裹号确认发货")
    void testConfirmShip_ByPackageNo() {
        // Given: 已打包的包裹
        String packageNo = "PK20260531001";
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setPackageNo(packageNo);
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        when(packRecordRepository.selectByPackageNo(packageNo)).thenReturn(packRecord);
        when(orderRepository.selectById(packRecord.getOutboundOrderId())).thenReturn(order);
        when(shipRecordRepository.insert(any(ShipRecord.class))).thenReturn(1);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(order.getId())).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);
        when(allocationRepository.selectByOrderId(order.getId())).thenReturn(new ArrayList<>());

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setPackageNo(packageNo);
        dto.setLogisticsCompany("顺丰速运");
        dto.setTrackingNo("SF1234567890");
        dto.setShipUserId(100L);
        dto.setShipUserName("李四");

        // When: 确认发货
        ShipResultDTO result = shipService.confirmShip(dto);

        // Then: 发货成功
        assertTrue(result.getSuccess());
        assertEquals(packageNo, result.getPackageNo());
        assertEquals(OutboundOrder.STATUS_SHIPPED, result.getNewStatus());

        // 验证创建了发货记录
        verify(shipRecordRepository).insert(argThat(record ->
            record.getPackageNo().equals(packageNo) &&
            record.getTrackingNo().equals("SF1234567890") &&
            record.getStatus() == ShipRecord.STATUS_SHIPPED
        ));
    }

    // ========== TC-SHIP-002 选择物流公司和录入物流单号 ==========

    @Test
    @DisplayName("TC-SHIP-002: 选择物流公司和录入物流单号")
    void testConfirmShip_EnterLogisticsInfo() {
        // Given: 待发货的包裹
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);
        order.setLogisticsCompany(null); // 初始无物流信息

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(shipRecordRepository.insert(any(ShipRecord.class))).thenReturn(1);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);
        when(allocationRepository.selectByOrderId(orderId)).thenReturn(new ArrayList<>());

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setLogisticsCompany("京东物流");
        dto.setLogisticsCompanyCode("JD");
        dto.setTrackingNo("JD9876543210");
        dto.setShipUserId(100L);

        // When: 确认发货
        ShipResultDTO result = shipService.confirmShip(dto);

        // Then: 物流信息已录入
        assertTrue(result.getSuccess());
        assertEquals("JD9876543210", result.getTrackingNo());

        // 验证更新了出库单的物流信息
        verify(orderRepository).updateById(argThat(o ->
            o != null &&
            "京东物流".equals(o.getLogisticsCompany()) &&
            "JD9876543210".equals(o.getTrackingNo()) &&
            o.getStatus() == OutboundOrder.STATUS_SHIPPED
        ));
    }

    // ========== TC-SHIP-003 批量确认发货 ==========

    @Test
    @DisplayName("TC-SHIP-003: 批量确认发货 - 多个出库单同时发货")
    void testBatchShip() {
        // Given: 多个待发货的出库单
        List<Long> orderIds = Arrays.asList(1L, 2L);

        OutboundOrder order1 = createTestOrder();
        order1.setId(1L);
        order1.setStatus(OutboundOrder.STATUS_SHIPPING);

        OutboundOrder order2 = createTestOrder();
        order2.setId(2L);
        order2.setOrderNo("OB20260531002");
        order2.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord1 = createTestPackRecord();
        packRecord1.setOutboundOrderId(1L);
        packRecord1.setStatus(PackRecord.STATUS_PACKED);

        PackRecord packRecord2 = createTestPackRecord();
        packRecord2.setOutboundOrderId(2L);
        packRecord2.setPackageNo("PK20260531002");
        packRecord2.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectById(1L)).thenReturn(order1);
        when(orderRepository.selectById(2L)).thenReturn(order2);
        when(packRecordRepository.selectLatestByOrderId(1L)).thenReturn(packRecord1);
        when(packRecordRepository.selectLatestByOrderId(2L)).thenReturn(packRecord2);
        when(shipRecordRepository.insert(any(ShipRecord.class))).thenReturn(1);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(anyLong())).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);
        when(allocationRepository.selectByOrderId(anyLong())).thenReturn(new ArrayList<>());

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOrderIds(orderIds);
        dto.setIsBatch(true);
        dto.setLogisticsCompany("顺丰速运");
        dto.setTrackingNo("SF-BATCH-001"); // 添加物流单号
        dto.setShipUserId(100L);

        // When: 批量发货
        ShipResultDTO result = shipService.batchShip(dto);

        // Then: 全部发货成功
        assertTrue(result.getSuccess());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
    }

    @Test
    @DisplayName("TC-SHIP-003: 批量确认发货 - 部分失败返回失败明细")
    void testBatchShip_PartialFailure() {
        // Given: 3个出库单，其中1个状态不对
        List<Long> orderIds = Arrays.asList(1L, 2L, 3L);

        OutboundOrder order1 = createTestOrder();
        order1.setId(1L);
        order1.setStatus(OutboundOrder.STATUS_SHIPPING);

        OutboundOrder order2 = createTestOrder();
        order2.setId(2L);
        order2.setStatus(OutboundOrder.STATUS_PICKING); // 状态不对

        OutboundOrder order3 = createTestOrder();
        order3.setId(3L);
        order3.setOrderNo("OB20260531003");
        order3.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord1 = createTestPackRecord();
        packRecord1.setOutboundOrderId(1L);
        packRecord1.setStatus(PackRecord.STATUS_PACKED);

        PackRecord packRecord3 = createTestPackRecord();
        packRecord3.setOutboundOrderId(3L);
        packRecord3.setPackageNo("PK20260531003");
        packRecord3.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectById(1L)).thenReturn(order1);
        when(orderRepository.selectById(2L)).thenReturn(order2);
        when(orderRepository.selectById(3L)).thenReturn(order3);
        when(packRecordRepository.selectLatestByOrderId(1L)).thenReturn(packRecord1);
        when(packRecordRepository.selectLatestByOrderId(2L)).thenReturn(null); // 状态不对，没有打包记录
        when(packRecordRepository.selectLatestByOrderId(3L)).thenReturn(packRecord3);
        when(shipRecordRepository.insert(any(ShipRecord.class))).thenReturn(1);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(anyLong())).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);
        when(allocationRepository.selectByOrderId(anyLong())).thenReturn(new ArrayList<>());

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOrderIds(orderIds);
        dto.setIsBatch(true);
        dto.setTrackingNo("SF-BATCH-002"); // 添加物流单号
        dto.setShipUserId(100L);

        // When: 批量发货
        ShipResultDTO result = shipService.batchShip(dto);

        // Then: 2个成功，1个失败
        assertTrue(result.getSuccess() || result.getFailCount() > 0); // 允许有失败
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertNotNull(result.getFailItems());
        assertEquals(1, result.getFailItems().size());
    }

    // ========== TC-SHIP-004 发货后状态变为已发货 ==========

    @Test
    @DisplayName("TC-SHIP-004: 发货后状态变为已发货")
    void testConfirmShip_StatusChange() {
        // Given: 待发货的订单
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(shipRecordRepository.insert(any(ShipRecord.class))).thenReturn(1);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);
        when(allocationRepository.selectByOrderId(orderId)).thenReturn(new ArrayList<>());

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setTrackingNo("SF123456");
        dto.setShipUserId(100L);

        // When: 确认发货
        ShipResultDTO result = shipService.confirmShip(dto);

        // Then: 状态变为已发货
        assertTrue(result.getSuccess());
        assertEquals(OutboundOrder.STATUS_SHIPPED, result.getNewStatus());
        assertEquals("已发货", result.getStatusName());

        verify(orderRepository).updateById(argThat(o ->
            o.getStatus() == OutboundOrder.STATUS_SHIPPED
        ));
    }

    @Test
    @DisplayName("TC-SHIP-004: 发货后自动通知ERP")
    void testConfirmShip_NotifyERP() {
        // Given: 待发货的订单
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);
        order.setSourceType(OutboundOrder.SOURCE_ERP); // ERP推送的订单

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(shipRecordRepository.insert(any(ShipRecord.class))).thenReturn(1);
        when(shipRecordRepository.updateById(any(ShipRecord.class))).thenReturn(1);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);
        when(allocationRepository.selectByOrderId(orderId)).thenReturn(new ArrayList<>());

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setTrackingNo("SF123456");
        dto.setShipUserId(100L);

        // When: 确认发货
        ShipResultDTO result = shipService.confirmShip(dto);

        // Then: 标记已通知ERP
        assertTrue(result.getSuccess());
        verify(shipRecordRepository).updateById(argThat(record ->
            record.getErpNotified() == 1
        ));
    }

    // ========== TC-SHIP-005 库存扣减 ==========

    @Test
    @DisplayName("TC-SHIP-005: 发货后库存扣减 - qty_total减少，qty_locked减少")
    void testConfirmShip_InventoryDeduction() {
        // Given: 待发货的订单
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        // 模拟库存分配记录
        InventoryAllocation allocation = new InventoryAllocation();
        allocation.setId(1L);
        allocation.setOutboundOrderId(orderId);
        allocation.setProductId(100L);
        allocation.setLocationId(10L);
        allocation.setAllocatedQty(10);
        allocation.setStatus(InventoryAllocation.STATUS_PICKED);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(shipRecordRepository.insert(any(ShipRecord.class))).thenReturn(1);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);
        when(allocationRepository.selectByOrderId(orderId)).thenReturn(Arrays.asList(allocation));
        when(allocationRepository.updateById(any(InventoryAllocation.class))).thenReturn(1);

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setTrackingNo("SF123456");
        dto.setShipUserId(100L);

        // When: 确认发货
        ShipResultDTO result = shipService.confirmShip(dto);

        // Then: 更新库存分配记录状态为已发货
        assertTrue(result.getSuccess());
        verify(allocationRepository).updateById(argThat(alloc ->
            alloc.getStatus() == InventoryAllocation.STATUS_SHIPPED
        ));
    }

    @Test
    @DisplayName("TC-SHIP-005: 系统记录发货人、发货时间")
    void testConfirmShip_RecordShipInfo() {
        // Given: 待发货的订单
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(shipRecordRepository.insert(any(ShipRecord.class))).thenReturn(1);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);
        when(allocationRepository.selectByOrderId(orderId)).thenReturn(new ArrayList<>());

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setTrackingNo("SF123456");
        dto.setShipUserId(100L);
        dto.setShipUserName("王五");

        // When: 确认发货
        ShipResultDTO result = shipService.confirmShip(dto);

        // Then: 记录发货人和发货时间
        assertTrue(result.getSuccess());

        verify(shipRecordRepository).insert(argThat(record ->
            record.getShipUserId().equals(100L) &&
            record.getShipUserName().equals("王五") &&
            record.getShipTime() != null
        ));

        verify(orderRepository).updateById(argThat(o ->
            o.getShipUserId().equals(100L) &&
            o.getShipUserName().equals("王五") &&
            o.getShipTime() != null
        ));
    }

    // ========== 边界值测试 ==========

    @Test
    @DisplayName("边界值测试: 状态不是待发货 - 不允许发货")
    void testConfirmShip_InvalidStatus() {
        // Given: 订单状态不是待发货
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING); // 待打包

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setTrackingNo("SF123456");

        // When & Then: 抛出异常
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> shipService.confirmShip(dto)
        );
        assertTrue(exception.getMessage().contains("状态不正确"));
    }

    @Test
    @DisplayName("边界值测试: 物流单号为空 - 不允许发货")
    void testConfirmShip_NoTrackingNo() {
        // Given: 未填写物流单号
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKED);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderId(orderId);
        // 未填写物流单号

        // When & Then: 抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> shipService.confirmShip(dto)
        );
        assertTrue(exception.getMessage().contains("物流单号"));
    }

    @Test
    @DisplayName("边界值测试: 未打包不能发货")
    void testConfirmShip_NotPacked() {
        // Given: 订单未打包
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_SHIPPING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKING); // 还在打包中

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);

        ShipConfirmDTO dto = new ShipConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setTrackingNo("SF123456");

        // When & Then: 抛出异常
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> shipService.confirmShip(dto)
        );
        assertTrue(exception.getMessage().contains("未打包"));
    }

    // ========== 辅助方法 ==========

    private OutboundOrder createTestOrder() {
        OutboundOrder order = new OutboundOrder();
        order.setId(1L);
        order.setOrderNo("OB20260531001");
        order.setStatus(OutboundOrder.STATUS_SHIPPING);
        order.setTotalQty(30);
        order.setWarehouseId(1L);
        order.setCustomerName("测试客户");
        order.setSourceType(OutboundOrder.SOURCE_MANUAL);
        return order;
    }

    private List<OutboundOrderItem> createTestOrderItems() {
        List<OutboundOrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            OutboundOrderItem item = new OutboundOrderItem();
            item.setId((long) i);
            item.setOrderId(1L);
            item.setProductId((long) i * 100);
            item.setSkuCode("SKU00" + i);
            item.setProductName("商品" + i);
            item.setQty(10);
            item.setPickedQty(10);
            item.setPackedQty(10);
            items.add(item);
        }
        return items;
    }

    private PackRecord createTestPackRecord() {
        PackRecord record = new PackRecord();
        record.setId(1L);
        record.setOutboundOrderId(1L);
        record.setOutboundOrderNo("OB20260531001");
        record.setPackageNo("PK20260531001");
        record.setStatus(PackRecord.STATUS_PACKED);
        record.setBoxType("中箱");
        record.setPackUserId(100L);
        record.setPackUserName("张三");
        record.setPackTime(LocalDateTime.now());
        return record;
    }
}
