package com.wms.outbound.service;

import com.wms.outbound.dto.*;
import com.wms.outbound.entity.*;
import com.wms.outbound.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 打包服务单元测试
 * TDD: 测试覆盖打包任务领取、打包确认、包装推荐场景
 *
 * 测试用例对照：
 * - TC-PACK-001: 查看待打包任务列表
 * - TC-PACK-002: 领取打包任务
 * - TC-PACK-003: 系统推荐包装箱型
 * - TC-PACK-004: 确认打包
 * - TC-PACK-005: 打包完成状态变更
 */
class PackServiceTest {

    @Mock
    private PackRecordRepository packRecordRepository;

    @Mock
    private OutboundOrderRepository orderRepository;

    @Mock
    private OutboundOrderItemRepository orderItemRepository;

    @Mock
    private BoxTypeRepository boxTypeRepository;

    private PackService packService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        packService = new PackService(
            packRecordRepository,
            orderRepository,
            orderItemRepository,
            boxTypeRepository
        );
    }

    // ========== TC-PACK-001 查看待打包任务列表 ==========

    @Test
    @DisplayName("TC-PACK-001: 查看待打包任务列表 - 显示状态为待打包的出库单")
    void testGetPendingPackTasks() {
        // Given: 有多个待打包的出库单
        Long warehouseId = 1L;

        List<OutboundOrder> orders = new ArrayList<>();
        OutboundOrder order1 = createTestOrder();
        order1.setStatus(OutboundOrder.STATUS_PACKING);
        orders.add(order1);

        when(orderRepository.selectByStatusAndWarehouse(OutboundOrder.STATUS_PACKING, warehouseId))
            .thenReturn(orders);
        when(orderItemRepository.selectByOrderId(anyLong())).thenReturn(createTestOrderItems());

        // When: 查看待打包任务列表
        List<PackTaskDetailDTO> result = packService.getPendingPackTasks(warehouseId);

        // Then: 返回待打包任务列表
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("OB20260531001", result.get(0).getOutboundOrderNo());

        verify(orderRepository).selectByStatusAndWarehouse(OutboundOrder.STATUS_PACKING, warehouseId);
    }

    @Test
    @DisplayName("TC-PACK-001: 查看待打包任务列表 - 按优先级排序")
    void testGetPendingPackTasks_SortByPriority() {
        // Given: 多个待打包订单，优先级不同
        Long warehouseId = 1L;

        // 注意：SQL已经按priority ASC排序，所以Mock返回的列表应该已经排好序
        List<OutboundOrder> orders = new ArrayList<>();

        // 紧急订单（priority=1）应该在前面
        OutboundOrder order1 = createTestOrder();
        order1.setId(1L);
        order1.setOrderNo("OB20260531001");
        order1.setPriority(OutboundOrder.PRIORITY_URGENT); // 紧急（数值1）
        orders.add(order1);

        // 普通优先级（priority=3）应该在后面
        OutboundOrder order2 = createTestOrder();
        order2.setId(2L);
        order2.setOrderNo("OB20260531002");
        order2.setPriority(OutboundOrder.PRIORITY_NORMAL); // 中优先级（数值3）
        orders.add(order2);

        when(orderRepository.selectByStatusAndWarehouse(anyInt(), anyLong())).thenReturn(orders);
        when(orderItemRepository.selectByOrderId(anyLong())).thenReturn(createTestOrderItems());

        // When: 查看待打包任务列表
        List<PackTaskDetailDTO> result = packService.getPendingPackTasks(warehouseId);

        // Then: 返回结果应该保持原有顺序（SQL已排序）
        assertNotNull(result);
        assertEquals(2, result.size());
        // 紧急订单（数值1）排在第一位
        assertEquals("OB20260531001", result.get(0).getOutboundOrderNo());
        // 普通订单（数值3）排在第二位
        assertEquals("OB20260531002", result.get(1).getOutboundOrderNo());
    }

    // ========== TC-PACK-002 领取打包任务 ==========

    @Test
    @DisplayName("TC-PACK-002: 领取打包任务 - 扫描出库单号确认打包订单")
    void testClaimPackTask() {
        // Given: 待打包的出库单
        String orderNo = "OB20260531001";
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);

        when(orderRepository.selectByOrderNo(orderNo)).thenReturn(order);
        when(orderItemRepository.selectByOrderId(order.getId())).thenReturn(createTestOrderItems());
        when(boxTypeRepository.selectAllEnabled()).thenReturn(createTestBoxTypes());
        when(packRecordRepository.insert(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);

        // When: 领取打包任务
        PackTaskDetailDTO result = packService.claimPackTask(orderNo, 100L, "张三");

        // Then: 返回商品清单和推荐包装
        assertNotNull(result);
        assertEquals(orderNo, result.getOutboundOrderNo());
        assertNotNull(result.getItems());
        assertFalse(result.getItems().isEmpty());
        assertNotNull(result.getRecommendedBoxes());

        // 验证创建了打包记录
        verify(packRecordRepository).insert(argThat(record ->
            record.getOutboundOrderNo().equals(orderNo) &&
            record.getStatus() == PackRecord.STATUS_PACKING &&
            record.getPackUserId().equals(100L)
        ));
    }

    @Test
    @DisplayName("TC-PACK-002: 领取打包任务 - 显示商品清单")
    void testClaimPackTask_ShowItemList() {
        // Given: 出库单有多个商品明细
        String orderNo = "OB20260531001";
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);

        List<OutboundOrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            OutboundOrderItem item = new OutboundOrderItem();
            item.setId((long) i);
            item.setProductId((long) i * 100);
            item.setSkuCode("SKU00" + i);
            item.setProductName("商品" + i);
            item.setQty(10);
            item.setPickedQty(10);
            items.add(item);
        }

        when(orderRepository.selectByOrderNo(orderNo)).thenReturn(order);
        when(orderItemRepository.selectByOrderId(order.getId())).thenReturn(items);
        when(boxTypeRepository.selectAllEnabled()).thenReturn(createTestBoxTypes());
        when(packRecordRepository.insert(any(PackRecord.class))).thenReturn(1);

        // When: 领取打包任务
        PackTaskDetailDTO result = packService.claimPackTask(orderNo, 100L, "张三");

        // Then: 显示所有商品明细
        assertNotNull(result.getItems());
        assertEquals(3, result.getItems().size());
        assertEquals("SKU001", result.getItems().get(0).getSkuCode());
    }

    // ========== TC-PACK-003 系统推荐包装箱型 ==========

    @Test
    @DisplayName("TC-PACK-003: 系统推荐包装箱型 - 根据商品尺寸重量推荐")
    void testRecommendBoxType() {
        // Given: 商品总重量和体积
        List<OutboundOrderItem> items = createTestOrderItems();
        List<BoxType> boxTypes = createTestBoxTypes();

        when(boxTypeRepository.selectAllEnabled()).thenReturn(boxTypes);

        // When: 计算推荐包装
        List<PackTaskDetailDTO.RecommendedBoxDTO> recommendations =
            packService.recommendBoxType(items, new BigDecimal("5.0"), new BigDecimal("0.02"));

        // Then: 返回推荐的箱型列表
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());

        // 推荐的箱型应该能满足重量和体积要求
        PackTaskDetailDTO.RecommendedBoxDTO best = recommendations.get(0);
        assertNotNull(best.getCode());
        assertNotNull(best.getReason());
    }

    @Test
    @DisplayName("TC-PACK-003: 系统推荐包装箱型 - 超重时推荐更大箱型")
    void testRecommendBoxType_OverWeight() {
        // Given: 商品重量较大
        List<OutboundOrderItem> items = createTestOrderItems();
        List<BoxType> boxTypes = createTestBoxTypes();

        when(boxTypeRepository.selectAllEnabled()).thenReturn(boxTypes);

        // When: 重量25kg，需要大箱
        List<PackTaskDetailDTO.RecommendedBoxDTO> recommendations =
            packService.recommendBoxType(items, new BigDecimal("25.0"), new BigDecimal("0.05"));

        // Then: 推荐承重更大的箱型
        assertNotNull(recommendations);
        assertTrue(recommendations.stream()
            .anyMatch(r -> r.getMaxWeight().compareTo(new BigDecimal("25.0")) >= 0));
    }

    // ========== TC-PACK-004 确认打包 ==========

    @Test
    @DisplayName("TC-PACK-004: 确认打包 - 录入实际使用的包装箱型")
    void testConfirmPack_EnterBoxType() {
        // Given: 打包任务
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKING);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(boxTypeRepository.selectByCode("M")).thenReturn(createTestBoxType("M", "中箱"));
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PackConfirmDTO dto = new PackConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setBoxTypeCode("M");
        dto.setWeight(new BigDecimal("8.5"));
        dto.setPackUserId(100L);
        dto.setPackUserName("张三");

        // When: 确认打包
        PackResultDTO result = packService.confirmPack(dto);

        // Then: 打包成功
        assertTrue(result.getSuccess());
        assertNotNull(result.getPackageNo());

        // 验证更新了打包记录
        verify(packRecordRepository).updateById(argThat(record ->
            record.getBoxTypeCode().equals("M") &&
            record.getWeight().compareTo(new BigDecimal("8.5")) == 0 &&
            record.getStatus() == PackRecord.STATUS_PACKED
        ));
    }

    @Test
    @DisplayName("TC-PACK-004: 确认打包 - 录入包裹重量")
    void testConfirmPack_EnterWeight() {
        // Given: 打包任务
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKING);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(boxTypeRepository.selectByCode(anyString())).thenReturn(createTestBoxType("M", "中箱"));
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PackConfirmDTO dto = new PackConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setBoxTypeCode("M");
        dto.setWeight(new BigDecimal("12.3")); // 称重设备录入
        dto.setPackUserId(100L);

        // When: 确认打包
        PackResultDTO result = packService.confirmPack(dto);

        // Then: 记录包裹重量
        assertTrue(result.getSuccess());
        verify(packRecordRepository).updateById(argThat(record ->
            record.getWeight().compareTo(new BigDecimal("12.3")) == 0
        ));
    }

    // ========== TC-PACK-005 打包完成状态变更 ==========

    @Test
    @DisplayName("TC-PACK-005: 打包完成后状态变为待发货")
    void testConfirmPack_StatusChangeToShipping() {
        // Given: 打包任务
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);
        order.setTotalQty(30);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKING);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(boxTypeRepository.selectByCode(anyString())).thenReturn(createTestBoxType("M", "中箱"));
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PackConfirmDTO dto = new PackConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setBoxTypeCode("M");
        dto.setWeight(new BigDecimal("10.0"));
        dto.setPackUserId(100L);

        // When: 确认打包
        PackResultDTO result = packService.confirmPack(dto);

        // Then: 订单状态变为待发货
        assertTrue(result.getSuccess());
        assertEquals(OutboundOrder.STATUS_SHIPPING, result.getNewStatus());

        verify(orderRepository).updateById(argThat(o ->
            o.getStatus() == OutboundOrder.STATUS_SHIPPING
        ));
    }

    @Test
    @DisplayName("TC-PACK-005: 打包完成 - 可以打印面单")
    void testConfirmPack_CanPrintLabel() {
        // Given: 打包完成
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);
        order.setLogisticsCompany("顺丰速运");

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKING);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(boxTypeRepository.selectByCode(anyString())).thenReturn(createTestBoxType("M", "中箱"));
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PackConfirmDTO dto = new PackConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setBoxTypeCode("M");
        dto.setWeight(new BigDecimal("10.0"));
        dto.setPackUserId(100L);

        // When: 确认打包
        PackResultDTO result = packService.confirmPack(dto);

        // Then: 返回包裹号（可用于打印面单）
        assertTrue(result.getSuccess());
        assertNotNull(result.getPackageNo());
        assertTrue(result.getPackageNo().startsWith("PK"));
    }

    // ========== 边界值测试 ==========

    @Test
    @DisplayName("边界值测试: 重复领取打包任务 - 提示已在打包中")
    void testClaimPackTask_AlreadyInPacking() {
        // Given: 订单已在打包中
        String orderNo = "OB20260531001";
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);

        PackRecord existingRecord = createTestPackRecord();
        existingRecord.setStatus(PackRecord.STATUS_PACKING);
        existingRecord.setPackUserId(200L);

        when(orderRepository.selectByOrderNo(orderNo)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(order.getId())).thenReturn(existingRecord);

        // When & Then: 抛出异常
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> packService.claimPackTask(orderNo, 100L, "张三")
        );
        assertTrue(exception.getMessage().contains("已在打包中"));
    }

    @Test
    @DisplayName("边界值测试: 打包重量超过箱型限制 - 警告但允许")
    void testConfirmPack_OverMaxWeight() {
        // Given: 打包重量超过箱型最大承重
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PACKING);

        PackRecord packRecord = createTestPackRecord();
        packRecord.setStatus(PackRecord.STATUS_PACKING);

        BoxType boxType = createTestBoxType("S", "小箱");
        boxType.setMaxWeight(new BigDecimal("5.0")); // 最大5kg

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(packRecordRepository.selectLatestByOrderId(orderId)).thenReturn(packRecord);
        when(boxTypeRepository.selectByCode("S")).thenReturn(boxType);
        when(packRecordRepository.updateById(any(PackRecord.class))).thenReturn(1);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(orderItemRepository.selectByOrderId(orderId)).thenReturn(createTestOrderItems());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PackConfirmDTO dto = new PackConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setBoxTypeCode("S");
        dto.setWeight(new BigDecimal("8.0")); // 实际8kg，超过限制
        dto.setPackUserId(100L);

        // When: 确认打包
        PackResultDTO result = packService.confirmPack(dto);

        // Then: 打包成功（警告但不阻止）
        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("边界值测试: 状态不是待打包 - 不允许打包")
    void testConfirmPack_InvalidStatus() {
        // Given: 订单状态不是待打包
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PICKING); // 拣货中

        when(orderRepository.selectById(orderId)).thenReturn(order);

        PackConfirmDTO dto = new PackConfirmDTO();
        dto.setOutboundOrderId(orderId);
        dto.setBoxTypeCode("M");
        dto.setWeight(new BigDecimal("10.0"));

        // When & Then: 抛出异常
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> packService.confirmPack(dto)
        );
        assertTrue(exception.getMessage().contains("状态不正确"));
    }

    // ========== 辅助方法 ==========

    private OutboundOrder createTestOrder() {
        OutboundOrder order = new OutboundOrder();
        order.setId(1L);
        order.setOrderNo("OB20260531001");
        order.setStatus(OutboundOrder.STATUS_PACKING);
        order.setTotalQty(30);
        order.setWarehouseId(1L);
        order.setCustomerName("测试客户");
        order.setReceiverName("收货人");
        order.setReceiverPhone("13800138000");
        order.setReceiverAddress("测试地址");
        order.setPriority(OutboundOrder.PRIORITY_NORMAL);
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
            item.setPackedQty(0);
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
        record.setStatus(PackRecord.STATUS_PACKING);
        record.setPackUserId(100L);
        record.setPackUserName("张三");
        record.setClaimTime(LocalDateTime.now());
        return record;
    }

    private List<BoxType> createTestBoxTypes() {
        List<BoxType> list = new ArrayList<>();

        BoxType s = new BoxType();
        s.setCode("S");
        s.setName("小箱");
        s.setVolume(new BigDecimal("0.009"));
        s.setMaxWeight(new BigDecimal("5.0"));
        s.setSortOrder(1);
        list.add(s);

        BoxType m = new BoxType();
        m.setCode("M");
        m.setName("中箱");
        m.setVolume(new BigDecimal("0.030"));
        m.setMaxWeight(new BigDecimal("15.0"));
        m.setSortOrder(2);
        list.add(m);

        BoxType l = new BoxType();
        l.setCode("L");
        l.setName("大箱");
        l.setVolume(new BigDecimal("0.070"));
        l.setMaxWeight(new BigDecimal("30.0"));
        l.setSortOrder(3);
        list.add(l);

        return list;
    }

    private BoxType createTestBoxType(String code, String name) {
        BoxType box = new BoxType();
        box.setCode(code);
        box.setName(name);
        box.setVolume(new BigDecimal("0.030"));
        box.setMaxWeight(new BigDecimal("15.0"));
        return box;
    }
}
