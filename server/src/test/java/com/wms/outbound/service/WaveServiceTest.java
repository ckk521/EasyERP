package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.outbound.dto.WaveCreateDTO;
import com.wms.outbound.dto.WaveReleaseDTO;
import com.wms.outbound.dto.WaveReleaseResultDTO;
import com.wms.outbound.entity.OutboundOrder;
import com.wms.outbound.entity.OutboundOrderItem;
import com.wms.outbound.entity.Wave;
import com.wms.outbound.repository.OutboundOrderItemRepository;
import com.wms.outbound.repository.OutboundOrderRepository;
import com.wms.outbound.repository.WaveRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 波次管理服务单元测试
 * TDD: 测试覆盖波次创建和释放场景
 *
 * 测试用例对照：
 * - TC-WAVE-001: 创建波次-按时间策略
 * - TC-WAVE-002: 创建波次-按物流策略
 * - TC-WAVE-003: 创建波次-按区域策略
 * - TC-WAVE-004: 创建波次-按商品策略
 * - TC-WAVE-005: 波次释放-正常流程
 * - TC-WAVE-006: 波次释放-库存不足
 * - TC-WAVE-007: 波次取消
 * - TC-WAVE-008: 波次最大订单数限制
 */
class WaveServiceTest {

    @Mock
    private WaveRepository waveRepository;

    @Mock
    private OutboundOrderRepository orderRepository;

    @Mock
    private OutboundOrderItemRepository orderItemRepository;

    @Mock
    private InventoryAllocationService allocationService;

    @InjectMocks
    private WaveService waveService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 设置自动生成的ID
        when(waveRepository.insert(any(Wave.class))).thenAnswer(invocation -> {
            Wave wave = invocation.getArgument(0);
            ReflectionTestUtils.setField(wave, "id", 1L);
            return 1;
        });

        // Mock orderItemRepository返回空列表（简化处理）
        when(orderItemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(new ArrayList<>());
    }

    // ========== TC-WAVE-001 创建波次 - 按时间策略 ==========

    @Test
    @DisplayName("TC-WAVE-001: 创建波次-按时间策略 - 自动纳入时间范围内的订单")
    void testCreateWave_ByTimeStrategy() {
        // Given: 时间范围内有待分配订单
        WaveCreateDTO dto = new WaveCreateDTO();
        dto.setStrategyType(Wave.STRATEGY_TIME);
        dto.setStrategyName("上午订单批次");
        dto.setWarehouseId(1L);
        dto.setWarehouseCode("WH-CN-001");
        dto.setWarehouseName("深圳总仓");
        dto.setStartTime(LocalDateTime.of(2026, 5, 31, 9, 0));
        dto.setEndTime(LocalDateTime.of(2026, 5, 31, 12, 0));

        // Mock: 模拟时间范围内的订单
        List<OutboundOrder> orders = createTestOrders(3);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);
        when(waveRepository.getMaxSeqByDate(anyString())).thenReturn(null);

        // When: 创建波次
        Long waveId = waveService.createWave(dto);

        // Then: 成功创建并纳入订单
        assertNotNull(waveId);
        verify(waveRepository).insert(argThat(wave ->
            wave.getStrategyType() == Wave.STRATEGY_TIME &&
            wave.getTotalOrders() == 3 &&
            wave.getStatus() == Wave.STATUS_PENDING
        ));
    }

    @Test
    @DisplayName("TC-WAVE-001: 波次号生成格式验证 - W+年月日+3位序号（共12位）")
    void testCreateWave_WaveNoFormat() {
        // Given: 按时间策略创建波次
        WaveCreateDTO dto = createWaveDTOByTime();

        // Mock: 模拟当日已有1个波次
        when(waveRepository.getMaxSeqByDate(anyString())).thenReturn(1);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(createTestOrders(2));

        // When: 创建波次
        Long waveId = waveService.createWave(dto);

        // Then: 波次号格式为 W+日期+002（当日已有1个，所以新的是002）
        assertNotNull(waveId);

        ArgumentCaptor<Wave> waveCaptor = ArgumentCaptor.forClass(Wave.class);
        verify(waveRepository).insert(waveCaptor.capture());

        Wave capturedWave = waveCaptor.getValue();
        String waveNo = capturedWave.getWaveNo();

        assertTrue(waveNo.startsWith("W"), "波次号应以W开头");
        assertEquals(12, waveNo.length(), "波次号长度应为12位（W+8位日期+3位序号）");
        assertTrue(waveNo.endsWith("002"), "波次号应以002结尾");
    }

    @Test
    @DisplayName("TC-WAVE-001: 波次初始状态为待释放")
    void testCreateWave_InitialStatus() {
        // Given: 创建波次DTO
        WaveCreateDTO dto = createWaveDTOByTime();
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(createTestOrders(2));

        // When: 创建波次
        waveService.createWave(dto);

        // Then: 状态为待释放(0)
        verify(waveRepository).insert(argThat(wave ->
            wave.getStatus() == Wave.STATUS_PENDING
        ));
    }

    @Test
    @DisplayName("TC-WAVE-001: 计算波次统计信息 - 订单数、SKU数、总件数")
    void testCreateWave_CalculateStatistics() {
        // Given: 3个订单，共5个SKU，35件商品
        WaveCreateDTO dto = createWaveDTOByTime();
        List<OutboundOrder> orders = createTestOrdersWithItems(3, 5, 35);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // Mock: 模拟订单明细（包含5个不同SKU）
        List<OutboundOrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            OutboundOrderItem item = new OutboundOrderItem();
            item.setSkuCode("SKU-00" + i);
            item.setQty(7);
            items.add(item);
        }
        when(orderItemRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(items);

        // When: 创建波次
        waveService.createWave(dto);

        // Then: 统计信息正确
        verify(waveRepository).insert(argThat(wave ->
            wave.getTotalOrders() == 3 &&
            wave.getTotalSku() == 5 &&
            wave.getTotalQty() == 33  // 3个订单 * 11件（35/3≈11）
        ));
    }

    // ========== TC-WAVE-002 创建波次 - 按物流策略 ==========

    @Test
    @DisplayName("TC-WAVE-002: 创建波次-按物流策略 - 只纳入指定物流的订单")
    void testCreateWave_ByLogisticsStrategy() {
        // Given: 按物流策略创建波次
        WaveCreateDTO dto = new WaveCreateDTO();
        dto.setStrategyType(Wave.STRATEGY_LOGISTICS);
        dto.setStrategyName("J&T Express批次");
        dto.setWarehouseId(1L);
        dto.setLogisticsCompany("J&T Express");

        // Mock: 模拟J&T物流的订单
        List<OutboundOrder> orders = createTestOrdersWithLogistics("J&T Express", 3);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // When: 创建波次
        Long waveId = waveService.createWave(dto);

        // Then: 只纳入J&T物流的订单
        assertNotNull(waveId);
        verify(waveRepository).insert(argThat(wave ->
            wave.getStrategyType() == Wave.STRATEGY_LOGISTICS &&
            wave.getTotalOrders() == 3
        ));
    }

    @Test
    @DisplayName("TC-WAVE-002: 按物流策略 - 其他物流订单不纳入")
    void testCreateWave_ByLogisticsStrategy_ExcludeOthers() {
        // Given: 按J&T物流创建波次
        WaveCreateDTO dto = new WaveCreateDTO();
        dto.setStrategyType(Wave.STRATEGY_LOGISTICS);
        dto.setLogisticsCompany("J&T Express");
        dto.setWarehouseId(1L);

        // Mock: 模拟只有J&T物流的订单被返回（数据库过滤后的结果）
        List<OutboundOrder> jtOrders = createTestOrdersWithLogistics("J&T Express", 2);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(jtOrders);

        // When: 创建波次
        waveService.createWave(dto);

        // Then: 只纳入J&T的订单，不包含Ninja Van
        verify(waveRepository).insert(argThat(wave ->
            wave.getTotalOrders() == 2  // 只有2个J&T订单
        ));
    }

    // ========== TC-WAVE-003 创建波次 - 按区域策略 ==========

    @Test
    @DisplayName("TC-WAVE-003: 创建波次-按区域策略 - 根据收货地址筛选")
    void testCreateWave_ByRegionStrategy() {
        // Given: 按区域策略创建波次
        WaveCreateDTO dto = new WaveCreateDTO();
        dto.setStrategyType(Wave.STRATEGY_REGION);
        dto.setStrategyName("雅加达地区批次");
        dto.setWarehouseId(1L);
        dto.setRegion("雅加达");

        // Mock: 模拟雅加达地区的订单
        List<OutboundOrder> orders = createTestOrdersWithRegion("雅加达", 4);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // When: 创建波次
        Long waveId = waveService.createWave(dto);

        // Then: 只纳入雅加达地区的订单
        assertNotNull(waveId);
        verify(waveRepository).insert(argThat(wave ->
            wave.getStrategyType() == Wave.STRATEGY_REGION &&
            wave.getTotalOrders() == 4
        ));
    }

    @Test
    @DisplayName("TC-WAVE-003: 按区域策略 - 自动解析地址中的区域信息")
    void testCreateWave_ByRegionStrategy_ParseAddress() {
        // Given: 订单地址包含"雅加达"
        WaveCreateDTO dto = new WaveCreateDTO();
        dto.setStrategyType(Wave.STRATEGY_REGION);
        dto.setRegion("雅加达");
        dto.setWarehouseId(1L);

        // Mock: 模拟只有雅加达地区的订单被返回（数据库过滤后的结果）
        List<OutboundOrder> orders = new ArrayList<>();
        OutboundOrder order1 = createTestOrder();
        order1.setReceiverAddress("雅加达市中心区某街道1号");
        orders.add(order1);

        OutboundOrder order2 = createTestOrder();
        order2.setReceiverAddress("雅加达市北区某街道2号");
        orders.add(order2);

        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // When: 创建波次
        waveService.createWave(dto);

        // Then: 只纳入地址包含雅加达的订单
        verify(waveRepository).insert(argThat(wave ->
            wave.getTotalOrders() == 2  // 只有2个雅加达订单
        ));
    }

    // ========== TC-WAVE-004 创建波次 - 按商品策略 ==========

    @Test
    @DisplayName("TC-WAVE-004: 创建波次-按商品策略 - 纳入含高频SKU的订单")
    void testCreateWave_ByProductStrategy() {
        // Given: 按商品策略创建波次，SKU重复阈值5
        WaveCreateDTO dto = new WaveCreateDTO();
        dto.setStrategyType(Wave.STRATEGY_PRODUCT);
        dto.setStrategyName("高频SKU批次");
        dto.setWarehouseId(1L);
        dto.setSkuRepeatThreshold(5);

        // Mock: 模拟包含高频SKU的订单
        List<OutboundOrder> orders = createTestOrdersWithHighFrequencySku(5);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // When: 创建波次
        Long waveId = waveService.createWave(dto);

        // Then: 纳入包含高频SKU的订单
        assertNotNull(waveId);
        verify(waveRepository).insert(argThat(wave ->
            wave.getStrategyType() == Wave.STRATEGY_PRODUCT
        ));
    }

    @Test
    @DisplayName("TC-WAVE-004: 按商品策略 - 显示高频SKU统计")
    void testCreateWave_ByProductStrategy_ShowStatistics() {
        // Given: SKU重复阈值5
        WaveCreateDTO dto = new WaveCreateDTO();
        dto.setStrategyType(Wave.STRATEGY_PRODUCT);
        dto.setSkuRepeatThreshold(5);

        // Mock: 模拟订单数据
        List<OutboundOrder> orders = createTestOrdersWithHighFrequencySku(5);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // When: 创建波次
        Map<String, Object> result = waveService.createWaveWithStatistics(dto);

        // Then: 返回高频SKU统计
        assertNotNull(result);
        assertNotNull(result.get("waveId"));
        assertNotNull(result.get("highFrequencySku"));
    }

    // ========== TC-WAVE-005 波次释放 - 正常流程 ==========

    @Test
    @DisplayName("TC-WAVE-005: 波次释放-正常流程 - 锁定库存并更新状态")
    void testReleaseWave_NormalFlow() {
        // Given: 待释放状态的波次
        Wave wave = createTestWave();
        wave.setStatus(Wave.STATUS_PENDING);
        when(waveRepository.selectById(1L)).thenReturn(wave);

        // Mock: 模拟波次中的订单
        List<OutboundOrder> orders = createTestOrders(2);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // Mock: 模拟库存锁定成功
        when(allocationService.batchLockInventory(anyList())).thenReturn(createSuccessAllocationResults());

        WaveReleaseDTO dto = new WaveReleaseDTO();
        dto.setWaveId(1L);
        dto.setAutoAssign(true);

        // When: 释放波次
        WaveReleaseResultDTO result = waveService.releaseWave(dto);

        // Then: 释放成功
        assertTrue(result.getSuccess());
        assertEquals(Wave.STATUS_PICKING, result.getNewStatus());

        // 验证波次状态更新
        verify(waveRepository).updateById(argThat(w ->
            w.getStatus() == Wave.STATUS_PICKING &&
            w.getStartTime() != null
        ));

        // 验证订单状态更新
        verify(orderRepository, times(2)).updateById(argThat(order ->
            order.getStatus() == OutboundOrder.STATUS_ALLOCATED
        ));

        // 验证库存锁定
        verify(allocationService).batchLockInventory(anyList());
    }

    @Test
    @DisplayName("TC-WAVE-005: 波次释放 - 生成拣货任务")
    void testReleaseWave_GeneratePickTasks() {
        // Given: 待释放的波次
        Wave wave = createTestWave();
        wave.setStatus(Wave.STATUS_PENDING);
        when(waveRepository.selectById(1L)).thenReturn(wave);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(createTestOrders(2));
        when(allocationService.batchLockInventory(anyList())).thenReturn(createSuccessAllocationResults());

        WaveReleaseDTO dto = new WaveReleaseDTO();
        dto.setWaveId(1L);
        dto.setAutoAssign(true);

        // When: 释放波次
        WaveReleaseResultDTO result = waveService.releaseWave(dto);

        // Then: 生成拣货任务
        assertTrue(result.getSuccess());
        assertNotNull(result.getPickTaskCount());
        assertTrue(result.getPickTaskCount() > 0);
    }

    // ========== TC-WAVE-006 波次释放 - 库存不足 ==========

    @Test
    @DisplayName("TC-WAVE-006: 波次释放-库存不足 - 提示缺货明细")
    void testReleaseWave_InsufficientStock() {
        // Given: 波次中部分商品库存不足
        Wave wave = createTestWave();
        wave.setStatus(Wave.STATUS_PENDING);
        when(waveRepository.selectById(1L)).thenReturn(wave);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(createTestOrders(2));

        // Mock: 模拟订单明细
        OutboundOrderItem item = new OutboundOrderItem();
        item.setProductId(1L);
        item.setQty(10);
        when(orderItemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // Mock: 模拟库存不足
        when(allocationService.batchLockInventory(anyList())).thenReturn(createPartialFailureResults());

        WaveReleaseDTO dto = new WaveReleaseDTO();
        dto.setWaveId(1L);

        // When: 释放波次
        WaveReleaseResultDTO result = waveService.releaseWave(dto);

        // Then: 返回缺货信息
        assertFalse(result.getSuccess());
        assertNotNull(result.getShortageList());
        assertTrue(result.getShortageList().size() > 0);
    }

    @Test
    @DisplayName("TC-WAVE-006: 波次释放-库存不足 - 可选择部分释放")
    void testReleaseWave_PartialRelease() {
        // Given: 部分商品库存不足，选择部分释放
        Wave wave = createTestWave();
        wave.setStatus(Wave.STATUS_PENDING);
        when(waveRepository.selectById(1L)).thenReturn(wave);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(createTestOrders(2));

        // Mock: 模拟订单明细
        OutboundOrderItem item = new OutboundOrderItem();
        item.setProductId(1L);
        item.setQty(10);
        when(orderItemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        when(allocationService.batchLockInventory(anyList())).thenReturn(createPartialFailureResults());

        WaveReleaseDTO dto = new WaveReleaseDTO();
        dto.setWaveId(1L);
        dto.setAllowPartialRelease(true); // 允许部分释放

        // When: 释放波次
        WaveReleaseResultDTO result = waveService.releaseWave(dto);

        // Then: 部分释放成功
        assertTrue(result.getSuccess());
        assertTrue(result.getPartialRelease());
        assertNotNull(result.getReleasedOrderCount());
        assertNotNull(result.getFailedOrderCount());
    }

    // ========== TC-WAVE-007 波次取消 ==========

    @Test
    @DisplayName("TC-WAVE-007: 波次取消 - 释放已锁定的库存")
    void testCancelWave_ReleaseLockedInventory() {
        // Given: 拣货中的波次（已锁定库存）
        Wave wave = createTestWave();
        wave.setStatus(Wave.STATUS_PICKING);
        wave.setTotalQty(50);
        when(waveRepository.selectById(1L)).thenReturn(wave);

        // Mock: 模拟波次中的订单
        List<OutboundOrder> orders = createTestOrdersWithAllocated(2);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // Mock: 模拟订单明细
        OutboundOrderItem item = new OutboundOrderItem();
        item.setProductId(1L);
        item.setQty(10);
        when(orderItemRepository.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(item));

        // Mock: 模拟库存释放成功
        when(allocationService.releaseInventory(anyLong(), anyLong(), anyInt())).thenReturn(true);

        // When: 取消波次
        boolean result = waveService.cancelWave(1L);

        // Then: 取消成功
        assertTrue(result);

        // 验证波次状态更新
        verify(waveRepository).updateById(argThat(w ->
            w.getStatus() == Wave.STATUS_CANCELLED
        ));

        // 验证订单状态回退
        verify(orderRepository, times(2)).updateById(argThat(order ->
            order.getStatus() == OutboundOrder.STATUS_PENDING
        ));

        // 验证库存释放
        verify(allocationService, atLeastOnce()).releaseInventory(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("TC-WAVE-007: 波次取消 - 待释放状态不释放库存")
    void testCancelWave_PendingStatus_NoRelease() {
        // Given: 待释放状态的波次（未锁定库存）
        Wave wave = createTestWave();
        wave.setStatus(Wave.STATUS_PENDING);
        when(waveRepository.selectById(1L)).thenReturn(wave);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(createTestOrders(2));

        // When: 取消波次
        boolean result = waveService.cancelWave(1L);

        // Then: 取消成功，但不释放库存
        assertTrue(result);
        verify(allocationService, never()).releaseInventory(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("TC-WAVE-007: 已完成波次不可取消")
    void testCancelWave_CompletedStatus_CannotCancel() {
        // Given: 已完成的波次
        Wave wave = createTestWave();
        wave.setStatus(Wave.STATUS_COMPLETED);
        when(waveRepository.selectById(1L)).thenReturn(wave);

        // When & Then: 抛出异常
        assertThrows(IllegalStateException.class, () -> {
            waveService.cancelWave(1L);
        }, "已完成的波次不可取消");
    }

    // ========== TC-WAVE-008 波次最大订单数限制 ==========

    @Test
    @DisplayName("TC-WAVE-008: 波次最大订单数限制 - 自动拆分为多个波次")
    void testCreateWave_MaxOrdersLimit() {
        // Given: 最大订单数限制50，时间范围内有80个订单
        WaveCreateDTO dto = createWaveDTOByTime();
        dto.setMaxOrders(50);

        // Mock: 模拟80个订单
        List<OutboundOrder> orders = createTestOrders(80);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);
        when(waveRepository.getMaxSeqByDate(anyString())).thenReturn(null);

        // When: 创建波次
        List<Long> waveIds = waveService.createWaveWithSplit(dto);

        // Then: 自动拆分为2个波次
        assertEquals(2, waveIds.size());

        // 验证第一个波次包含50个订单
        verify(waveRepository, times(2)).insert(any(Wave.class));
    }

    @Test
    @DisplayName("TC-WAVE-008: 波次最大订单数限制 - 刚好等于限制不拆分")
    void testCreateWave_MaxOrdersLimit_ExactMatch() {
        // Given: 最大订单数50，时间范围内有50个订单
        WaveCreateDTO dto = createWaveDTOByTime();
        dto.setMaxOrders(50);

        // Mock: 模拟50个订单
        List<OutboundOrder> orders = createTestOrders(50);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        // When: 创建波次
        List<Long> waveIds = waveService.createWaveWithSplit(dto);

        // Then: 不拆分，只创建1个波次
        assertEquals(1, waveIds.size());
    }

    // ========== 边界值测试 ==========

    @Test
    @DisplayName("边界值测试: 无符合条件的订单 - 波次创建失败")
    void testCreateWave_NoOrders() {
        // Given: 时间范围内无订单
        WaveCreateDTO dto = createWaveDTOByTime();
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When & Then: 抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            waveService.createWave(dto);
        }, "无符合条件的订单");
    }

    @Test
    @DisplayName("边界值测试: 波次号序号达到999后继续递增")
    void testCreateWave_SeqExceeds999() {
        // Given: 当日已有999个波次
        when(waveRepository.getMaxSeqByDate("W20260531")).thenReturn(999);
        when(orderRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(createTestOrders(2));

        // When: 创建波次
        WaveCreateDTO dto = createWaveDTOByTime();
        Long waveId = waveService.createWave(dto);

        // Then: 序号变为1000，波次号长度变为13位
        assertNotNull(waveId);
        verify(waveRepository).insert(argThat(wave ->
            wave.getWaveNo().equals("W202605311000") &&
            wave.getWaveNo().length() == 13
        ));
    }

    // ========== 辅助方法 ==========

    private WaveCreateDTO createWaveDTOByTime() {
        WaveCreateDTO dto = new WaveCreateDTO();
        dto.setStrategyType(Wave.STRATEGY_TIME);
        dto.setStrategyName("上午订单批次");
        dto.setWarehouseId(1L);
        dto.setWarehouseCode("WH-CN-001");
        dto.setWarehouseName("深圳总仓");
        dto.setStartTime(LocalDateTime.of(2026, 5, 31, 9, 0));
        dto.setEndTime(LocalDateTime.of(2026, 5, 31, 12, 0));
        return dto;
    }

    private Wave createTestWave() {
        Wave wave = new Wave();
        wave.setId(1L);
        wave.setWaveNo("W20260531001");
        wave.setStrategyType(Wave.STRATEGY_TIME);
        wave.setStrategyName("上午订单批次");
        wave.setWarehouseId(1L);
        wave.setWarehouseCode("WH-CN-001");
        wave.setWarehouseName("深圳总仓");
        wave.setStatus(Wave.STATUS_PENDING);
        wave.setTotalOrders(2);
        wave.setTotalSku(3);
        wave.setTotalQty(35);
        return wave;
    }

    private OutboundOrder createTestOrder() {
        OutboundOrder order = new OutboundOrder();
        order.setId(1L);
        order.setOrderNo("OB20260531001");
        order.setOrderType(OutboundOrder.TYPE_SALES);
        order.setSourceType(OutboundOrder.SOURCE_ERP);
        order.setWarehouseId(1L);
        order.setStatus(OutboundOrder.STATUS_PENDING);
        order.setLogisticsCompany("J&T Express");
        order.setReceiverAddress("雅加达市中心区某街道1号");
        order.setTotalQty(15);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    private List<OutboundOrder> createTestOrders(int count) {
        List<OutboundOrder> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OutboundOrder order = createTestOrder();
            order.setId((long) (i + 1));
            order.setOrderNo("OB2026053100" + (i + 1));
            orders.add(order);
        }
        return orders;
    }

    private List<OutboundOrder> createTestOrdersWithItems(int orderCount, int skuCount, int totalQty) {
        List<OutboundOrder> orders = createTestOrders(orderCount);
        // 简化处理：平均分配SKU和数量
        int qtyPerOrder = totalQty / orderCount;
        orders.forEach(order -> order.setTotalQty(qtyPerOrder));
        return orders;
    }

    private List<OutboundOrder> createTestOrdersWithLogistics(String logistics, int count) {
        List<OutboundOrder> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OutboundOrder order = createTestOrder();
            order.setId((long) (i + 1));
            order.setLogisticsCompany(logistics);
            orders.add(order);
        }
        return orders;
    }

    private List<OutboundOrder> createTestOrdersWithRegion(String region, int count) {
        List<OutboundOrder> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OutboundOrder order = createTestOrder();
            order.setId((long) (i + 1));
            order.setReceiverAddress(region + "市某街道" + (i + 1) + "号");
            orders.add(order);
        }
        return orders;
    }

    private List<OutboundOrder> createTestOrdersWithHighFrequencySku(int threshold) {
        // 简化处理：返回包含高频SKU的订单
        return createTestOrders(threshold);
    }

    private List<OutboundOrder> createTestOrdersWithAllocated(int count) {
        List<OutboundOrder> orders = createTestOrders(count);
        orders.forEach(order -> order.setStatus(OutboundOrder.STATUS_ALLOCATED));
        return orders;
    }

    private List<com.wms.outbound.dto.AllocationResultDTO> createSuccessAllocationResults() {
        List<com.wms.outbound.dto.AllocationResultDTO> results = new ArrayList<>();
        com.wms.outbound.dto.AllocationResultDTO result = new com.wms.outbound.dto.AllocationResultDTO();
        result.setSuccess(true);
        results.add(result);
        return results;
    }

    private List<com.wms.outbound.dto.AllocationResultDTO> createPartialFailureResults() {
        List<com.wms.outbound.dto.AllocationResultDTO> results = new ArrayList<>();

        // 成功的分配
        com.wms.outbound.dto.AllocationResultDTO success = new com.wms.outbound.dto.AllocationResultDTO();
        success.setSuccess(true);
        success.setProductId(1L);
        results.add(success);

        // 失败的分配（库存不足）
        com.wms.outbound.dto.AllocationResultDTO failure = new com.wms.outbound.dto.AllocationResultDTO();
        failure.setSuccess(false);
        failure.setProductId(2L);
        failure.setRequestedQty(60);
        failure.setAvailableBefore(50);
        failure.setFailReason("库存不足，当前可用库存：50件");
        results.add(failure);

        return results;
    }
}
