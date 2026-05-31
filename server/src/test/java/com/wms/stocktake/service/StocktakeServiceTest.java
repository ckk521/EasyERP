package com.wms.stocktake.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.inbound.entity.Inventory;
import com.wms.inventory.repository.InventoryRepositoryExt;
import com.wms.stocktake.dto.*;
import com.wms.stocktake.entity.StocktakeItem;
import com.wms.stocktake.entity.StocktakeOrder;
import com.wms.stocktake.repository.StocktakeItemRepository;
import com.wms.stocktake.repository.StocktakeOrderRepository;
import com.wms.system.entity.BaseLocation;
import com.wms.system.entity.BaseProduct;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.repository.BaseLocationRepository;
import com.wms.system.repository.BaseProductRepository;
import com.wms.system.repository.SysWarehouseRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 盘点服务单元测试 - TDD完整版
 * 测试覆盖需求文档所有核心场景
 *
 * 测试用例映射：
 * - TC-STK-001 ~ TC-STK-008: 盘点单创建测试
 * - TC-STK-009 ~ TC-STK-021: 盘点作业测试
 * - TC-STK-022 ~ TC-STK-030: 差异处理测试
 */
class StocktakeServiceTest {

    @Mock
    private StocktakeOrderRepository orderRepository;

    @Mock
    private StocktakeItemRepository itemRepository;

    @Mock
    private InventoryRepositoryExt inventoryRepository;

    @Mock
    private SysWarehouseRepository warehouseRepository;

    @Mock
    private BaseLocationRepository locationRepository;

    @Mock
    private BaseProductRepository productRepository;

    @InjectMocks
    private StocktakeServiceImpl stocktakeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========== TC-STK-001 创建全盘盘点单 - 明盘模式 ==========

    @Test
    @DisplayName("TC-STK-001: 创建全盘盘点单-明盘模式 - 自动生成盘点单号并加载所有库存")
    void testCreateFullStocktake_OpenMode() {
        // Given: 全盘参数，明盘模式
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setWarehouseId(1L);
        dto.setStocktakeType(StocktakeOrder.TYPE_FULL);
        dto.setBlindMode(StocktakeOrder.BLIND_MODE_OFF);
        dto.setScopeType("all");
        dto.setPlanDate(LocalDate.now().plusDays(1));

        // Mock仓库
        SysWarehouse warehouse = createTestWarehouse(1L, "WH-CN-001", "深圳总仓");
        when(warehouseRepository.selectById(1L)).thenReturn(warehouse);

        // Mock库存数据 - 156条库存记录
        List<Inventory> inventories = createTestInventories(156);
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(inventories);

        // Mock单号生成
        when(orderRepository.getMaxSeqByDate(anyString())).thenReturn(null);

        // Mock insert操作，模拟ID填充
        doAnswer(invocation -> {
            StocktakeOrder order = invocation.getArgument(0);
            order.setId(1L);
            return 1;
        }).when(orderRepository).insert(any(StocktakeOrder.class));

        // When: 创建盘点单
        Long orderId = stocktakeService.createOrder(dto, 1L, "admin");

        // Then: 返回盘点单ID，自动生成单号
        assertNotNull(orderId);
        verify(orderRepository).insert(any(StocktakeOrder.class));

        // 验证创建了盘点明细（156条）
        verify(itemRepository, times(156)).insert(any(StocktakeItem.class));

        // 验证盲盘模式设置正确（明盘）
        ArgumentCaptor<StocktakeOrder> orderCaptor = ArgumentCaptor.forClass(StocktakeOrder.class);
        verify(orderRepository).insert(orderCaptor.capture());
        assertEquals(StocktakeOrder.BLIND_MODE_OFF, orderCaptor.getValue().getBlindMode());
        assertEquals(156, orderCaptor.getValue().getTotalItems());
        assertEquals(StocktakeOrder.STATUS_PENDING, orderCaptor.getValue().getStatus());
    }

    // ========== TC-STK-002 创建全盘盘点单 - 盲盘模式 ==========

    @Test
    @DisplayName("TC-STK-002: 创建全盘盘点单-盲盘模式 - 系统数量隐藏")
    void testCreateFullStocktake_BlindMode() {
        // Given: 全盘参数，盲盘模式
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setWarehouseId(1L);
        dto.setStocktakeType(StocktakeOrder.TYPE_FULL);
        dto.setBlindMode(StocktakeOrder.BLIND_MODE_ON);
        dto.setScopeType("all");
        dto.setPlanDate(LocalDate.now().plusDays(1));

        SysWarehouse warehouse = createTestWarehouse(1L, "WH-CN-001", "深圳总仓");
        when(warehouseRepository.selectById(1L)).thenReturn(warehouse);

        List<Inventory> inventories = createTestInventories(5);
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(inventories);

        when(orderRepository.getMaxSeqByDate(anyString())).thenReturn(null);

        // Mock insert操作，模拟ID填充
        doAnswer(invocation -> {
            StocktakeOrder order = invocation.getArgument(0);
            order.setId(1L);
            return 1;
        }).when(orderRepository).insert(any(StocktakeOrder.class));

        // When: 创建盘点单
        Long orderId = stocktakeService.createOrder(dto, 1L, "admin");

        // Then: 盲盘模式设置正确
        assertNotNull(orderId);
        ArgumentCaptor<StocktakeOrder> orderCaptor = ArgumentCaptor.forClass(StocktakeOrder.class);
        verify(orderRepository).insert(orderCaptor.capture());
        assertEquals(StocktakeOrder.BLIND_MODE_ON, orderCaptor.getValue().getBlindMode());
    }

    // ========== TC-STK-003 创建抽盘盘点单 - 按库区范围 ==========

    @Test
    @DisplayName("TC-STK-003: 创建抽盘盘点单-按库区范围 - 只加载指定库区库存")
    void testCreateSampleStocktake_ByZoneRange() {
        // Given: 抽盘参数-按库区范围
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setWarehouseId(1L);
        dto.setStocktakeType(StocktakeOrder.TYPE_SAMPLE);
        dto.setBlindMode(StocktakeOrder.BLIND_MODE_OFF);
        dto.setScopeType("zone");
        dto.setZoneIds(Arrays.asList(1L, 2L)); // 仓储A部和仓储B部
        dto.setPlanDate(LocalDate.now().plusDays(1));

        SysWarehouse warehouse = createTestWarehouse(1L, "WH-CN-001", "深圳总仓");
        when(warehouseRepository.selectById(1L)).thenReturn(warehouse);

        // Mock库区下的库位
        List<BaseLocation> zoneALocations = createTestLocations(1L, "ZONE-A", 5);
        List<BaseLocation> zoneBLocations = createTestLocations(2L, "ZONE-B", 5);
        when(locationRepository.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(zoneALocations)
                .thenReturn(zoneBLocations);

        // Mock库存（仓储A部有3条，仓储B部有2条）
        List<Inventory> inventoriesZoneA = createTestInventoriesForZone(3, 1L);
        List<Inventory> inventoriesZoneB = createTestInventoriesForZone(2, 2L);
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(inventoriesZoneA)
                .thenReturn(inventoriesZoneB);

        when(orderRepository.getMaxSeqByDate(anyString())).thenReturn(null);

        // Mock insert操作，模拟ID填充
        doAnswer(invocation -> {
            StocktakeOrder order = invocation.getArgument(0);
            order.setId(1L);
            return 1;
        }).when(orderRepository).insert(any(StocktakeOrder.class));

        // When
        Long orderId = stocktakeService.createOrder(dto, 1L, "admin");

        // Then: 只创建指定库区的盘点明细
        assertNotNull(orderId);
        verify(locationRepository, atLeastOnce()).selectList(any(LambdaQueryWrapper.class));
    }

    // ========== TC-STK-004 创建抽盘盘点单 - 按指定商品 ==========

    @Test
    @DisplayName("TC-STK-004: 创建抽盘盘点单-按指定商品 - 只加载选中的SKU")
    void testCreateSampleStocktake_BySpecificSku() {
        // Given: 抽盘参数-指定SKU
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setWarehouseId(1L);
        dto.setStocktakeType(StocktakeOrder.TYPE_SAMPLE);
        dto.setScopeType("sku");
        dto.setSkuCodes(Arrays.asList("SKU-009", "SKU-010"));
        dto.setPlanDate(LocalDate.now().plusDays(1));

        SysWarehouse warehouse = createTestWarehouse(1L, "WH-CN-001", "深圳总仓");
        when(warehouseRepository.selectById(1L)).thenReturn(warehouse);

        // Mock指定SKU的库存（SKU-009在2个库位，SKU-010在1个库位）
        List<Inventory> inventories = new ArrayList<>();
        inventories.add(createTestInventory(1L, "SKU-009", "海飞丝洗发水", 100, "A-R01-L01"));
        inventories.add(createTestInventory(2L, "SKU-009", "海飞丝洗发水", 50, "A-R01-L02"));
        inventories.add(createTestInventory(3L, "SKU-010", "玉兰油面霜", 77, "B-R01-L01"));
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(inventories);

        when(orderRepository.getMaxSeqByDate(anyString())).thenReturn(null);

        // Mock insert操作，模拟ID填充
        doAnswer(invocation -> {
            StocktakeOrder order = invocation.getArgument(0);
            order.setId(1L);
            return 1;
        }).when(orderRepository).insert(any(StocktakeOrder.class));

        // When
        Long orderId = stocktakeService.createOrder(dto, 1L, "admin");

        // Then: 只创建3条盘点明细（SKU-009两条+SKU-010一条）
        assertNotNull(orderId);
        verify(itemRepository, times(3)).insert(any(StocktakeItem.class));

        ArgumentCaptor<StocktakeOrder> orderCaptor = ArgumentCaptor.forClass(StocktakeOrder.class);
        verify(orderRepository).insert(orderCaptor.capture());
        assertEquals(3, orderCaptor.getValue().getTotalItems());
    }

    // ========== TC-STK-007 创建盘点单 - 验证必填项 ==========

    @Test
    @DisplayName("TC-STK-007: 创建盘点单-验证必填项 - 仓库为空应报错")
    void testCreateStocktake_WarehouseRequired() {
        // Given: 仓库为空
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setStocktakeType(StocktakeOrder.TYPE_FULL);
        dto.setPlanDate(LocalDate.now());

        // When & Then: 应抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            stocktakeService.createOrder(dto, 1L, "admin");
        });

        assertTrue(exception.getMessage().contains("仓库"));
    }

    @Test
    @DisplayName("TC-STK-007: 创建盘点单-验证必填项 - 盘点类型为空应报错")
    void testCreateStocktake_TypeRequired() {
        // Given: 盘点类型为空
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setWarehouseId(1L);
        dto.setPlanDate(LocalDate.now());

        when(warehouseRepository.selectById(1L)).thenReturn(createTestWarehouse(1L, "WH001", "测试仓库"));

        // When & Then: 应抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            stocktakeService.createOrder(dto, 1L, "admin");
        });

        assertTrue(exception.getMessage().contains("盘点类型"));
    }

    // ========== TC-STK-008 创建盘点单 - 空仓库验证 ==========

    @Test
    @DisplayName("TC-STK-008: 创建盘点单-空仓库验证 - 无库存应报错")
    void testCreateStocktake_EmptyWarehouse() {
        // Given: 仓库无库存
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setWarehouseId(1L);
        dto.setStocktakeType(StocktakeOrder.TYPE_FULL);
        dto.setScopeType("all");
        dto.setPlanDate(LocalDate.now());

        SysWarehouse warehouse = createTestWarehouse(1L, "WH001", "空仓库");
        when(warehouseRepository.selectById(1L)).thenReturn(warehouse);
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When & Then: 应抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            stocktakeService.createOrder(dto, 1L, "admin");
        });

        assertTrue(exception.getMessage().contains("无库存"));
    }

    // ========== TC-STK-009 开始盘点 - 进入作业页面 ==========

    @Test
    @DisplayName("TC-STK-009: 开始盘点-进入作业页面 - 状态变更为盘点中")
    void testCountItem_StatusChangesToCounting() {
        // Given: 待盘点状态的盘点明细
        StocktakeItem item = createTestStocktakeItem(1L, 1L, "SKU-009", 100);
        item.setStatus(StocktakeItem.STATUS_PENDING);

        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_PENDING);
        order.setTotalItems(10);

        when(itemRepository.selectById(1L)).thenReturn(item);
        when(orderRepository.selectById(1L)).thenReturn(order);
        when(itemRepository.countByOrderIdAndCounted(1L)).thenReturn(1);

        StocktakeCountDTO dto = new StocktakeCountDTO();
        dto.setItemId(1L);
        dto.setCountedQty(100);

        // When: 开始盘点
        stocktakeService.countItem(dto, 1L, "admin");

        // Then: 盘点单状态变更为"盘点中"
        ArgumentCaptor<StocktakeOrder> orderCaptor = ArgumentCaptor.forClass(StocktakeOrder.class);
        verify(orderRepository).updateById(orderCaptor.capture());
        assertEquals(StocktakeOrder.STATUS_COUNTING, orderCaptor.getValue().getStatus());
        assertNotNull(orderCaptor.getValue().getStartTime());
    }

    // ========== TC-STK-011 盘点作业 - 明盘录入（数量一致） ==========

    @Test
    @DisplayName("TC-STK-011: 盘点作业-明盘录入数量一致 - 差异为0")
    void testCountItem_QtyMatch() {
        // Given: 系统数量50件，实盘50件
        StocktakeItem item = createTestStocktakeItem(1L, 1L, "SKU-009", 50);
        item.setStatus(StocktakeItem.STATUS_PENDING);

        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_COUNTING);

        when(itemRepository.selectById(1L)).thenReturn(item);
        when(orderRepository.selectById(1L)).thenReturn(order);
        when(itemRepository.countByOrderIdAndCounted(1L)).thenReturn(1);

        StocktakeCountDTO dto = new StocktakeCountDTO();
        dto.setItemId(1L);
        dto.setCountedQty(50);

        // When: 录入实盘数量50
        stocktakeService.countItem(dto, 1L, "admin");

        // Then: 差异为0，状态变更为已盘点
        ArgumentCaptor<StocktakeItem> itemCaptor = ArgumentCaptor.forClass(StocktakeItem.class);
        verify(itemRepository).updateById(itemCaptor.capture());
        assertEquals(50, itemCaptor.getValue().getCountedQty());
        assertEquals(0, itemCaptor.getValue().getDiffQty());
        assertEquals(StocktakeItem.STATUS_COUNTED, itemCaptor.getValue().getStatus());
    }

    // ========== TC-STK-012 盘点作业 - 明盘录入（盘亏） ==========

    @Test
    @DisplayName("TC-STK-012: 盘点作业-明盘录入盘亏 - 差异为负数")
    void testCountItem_Loss() {
        // Given: 系统数量50件，实盘45件
        StocktakeItem item = createTestStocktakeItem(1L, 1L, "SKU-009", 50);
        item.setStatus(StocktakeItem.STATUS_PENDING);

        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_COUNTING);

        when(itemRepository.selectById(1L)).thenReturn(item);
        when(orderRepository.selectById(1L)).thenReturn(order);
        when(itemRepository.countByOrderIdAndCounted(1L)).thenReturn(1);

        StocktakeCountDTO dto = new StocktakeCountDTO();
        dto.setItemId(1L);
        dto.setCountedQty(45);
        dto.setDiffReason("loss");
        dto.setDiffRemark("外包装破损，已报废");

        // When: 录入实盘数量45
        stocktakeService.countItem(dto, 1L, "admin");

        // Then: 差异为-5（盘亏）
        ArgumentCaptor<StocktakeItem> itemCaptor = ArgumentCaptor.forClass(StocktakeItem.class);
        verify(itemRepository).updateById(itemCaptor.capture());
        assertEquals(45, itemCaptor.getValue().getCountedQty());
        assertEquals(-5, itemCaptor.getValue().getDiffQty());
        assertEquals("loss", itemCaptor.getValue().getDiffReason());
        assertEquals("外包装破损，已报废", itemCaptor.getValue().getDiffRemark());
    }

    // ========== TC-STK-013 盘点作业 - 明盘录入（盘盈） ==========

    @Test
    @DisplayName("TC-STK-013: 盘点作业-明盘录入盘盈 - 差异为正数")
    void testCountItem_Profit() {
        // Given: 系统数量50件，实盘55件
        StocktakeItem item = createTestStocktakeItem(1L, 1L, "SKU-009", 50);
        item.setStatus(StocktakeItem.STATUS_PENDING);

        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_COUNTING);

        when(itemRepository.selectById(1L)).thenReturn(item);
        when(orderRepository.selectById(1L)).thenReturn(order);
        when(itemRepository.countByOrderIdAndCounted(1L)).thenReturn(1);

        StocktakeCountDTO dto = new StocktakeCountDTO();
        dto.setItemId(1L);
        dto.setCountedQty(55);
        dto.setDiffReason("profit");
        dto.setDiffRemark("可能是其他库位错放");

        // When: 录入实盘数量55
        stocktakeService.countItem(dto, 1L, "admin");

        // Then: 差异为+5（盘盈）
        ArgumentCaptor<StocktakeItem> itemCaptor = ArgumentCaptor.forClass(StocktakeItem.class);
        verify(itemRepository).updateById(itemCaptor.capture());
        assertEquals(55, itemCaptor.getValue().getCountedQty());
        assertEquals(5, itemCaptor.getValue().getDiffQty());
        assertEquals("profit", itemCaptor.getValue().getDiffReason());
    }

    // ========== TC-STK-019 盘点作业 - 按批次盘点 ==========

    @Test
    @DisplayName("TC-STK-019: 盘点作业-同一库位同SKU多批次 - 分别盘点")
    void testCreateStocktake_MultipleBatches() {
        // Given: 同一库位同一SKU有2个批次
        StocktakeCreateDTO dto = new StocktakeCreateDTO();
        dto.setWarehouseId(1L);
        dto.setStocktakeType(StocktakeOrder.TYPE_FULL);
        dto.setScopeType("all");
        dto.setPlanDate(LocalDate.now());

        SysWarehouse warehouse = createTestWarehouse(1L, "WH001", "深圳总仓");
        when(warehouseRepository.selectById(1L)).thenReturn(warehouse);

        // Mock库存：同一SKU-009，同一库位，2个批次
        List<Inventory> inventories = new ArrayList<>();
        Inventory inv1 = createTestInventory(1L, "SKU-009", "海飞丝洗发水", 30, "A-R01-L01");
        inv1.setBatchNo("B001");
        Inventory inv2 = createTestInventory(2L, "SKU-009", "海飞丝洗发水", 20, "A-R01-L01");
        inv2.setBatchNo("B002");
        inventories.add(inv1);
        inventories.add(inv2);
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(inventories);

        when(orderRepository.getMaxSeqByDate(anyString())).thenReturn(null);

        // Mock insert操作，模拟ID填充
        doAnswer(invocation -> {
            StocktakeOrder order = invocation.getArgument(0);
            order.setId(1L);
            return 1;
        }).when(orderRepository).insert(any(StocktakeOrder.class));

        // When: 创建盘点单
        Long orderId = stocktakeService.createOrder(dto, 1L, "admin");

        // Then: 创建2条盘点明细，区分批次
        assertNotNull(orderId);
        verify(itemRepository, times(2)).insert(any(StocktakeItem.class));

        // 验证两条明细的批次号不同
        ArgumentCaptor<StocktakeItem> itemCaptor = ArgumentCaptor.forClass(StocktakeItem.class);
        verify(itemRepository, times(2)).insert(itemCaptor.capture());
        List<StocktakeItem> items = itemCaptor.getAllValues();
        assertEquals("B001", items.get(0).getBatchNo());
        assertEquals("B002", items.get(1).getBatchNo());
    }

    // ========== TC-STK-020 完成盘点 ==========

    @Test
    @DisplayName("TC-STK-020: 完成盘点 - 计算准确率并更新状态")
    void testFinishStocktake_CalculateAccuracy() {
        // Given: 盘点单，全部已盘点
        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_COUNTING);
        order.setTotalItems(156);
        order.setCountedItems(156);

        when(orderRepository.selectById(1L)).thenReturn(order);
        when(itemRepository.countByOrderIdAndDiff(1L)).thenReturn(14); // 14条有差异

        // When: 完成盘点
        stocktakeService.finishOrder(1L);

        // Then: 准确率 = (156-14)/156 * 100 = 91.03%
        ArgumentCaptor<StocktakeOrder> orderCaptor = ArgumentCaptor.forClass(StocktakeOrder.class);
        verify(orderRepository).updateById(orderCaptor.capture());
        assertEquals(StocktakeOrder.STATUS_REVIEWING, orderCaptor.getValue().getStatus());
        assertEquals(14, orderCaptor.getValue().getDiffItems());

        BigDecimal expectedAccuracy = BigDecimal.valueOf(142)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(156), 2, BigDecimal.ROUND_HALF_UP);
        assertEquals(expectedAccuracy, orderCaptor.getValue().getAccuracyRate());
        assertNotNull(orderCaptor.getValue().getFinishTime());
    }

    @Test
    @DisplayName("TC-STK-020: 完成盘点 - 未全部盘点应报错")
    void testFinishStocktake_NotAllCounted() {
        // Given: 盘点单，未全部盘点
        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_COUNTING);
        order.setTotalItems(156);
        order.setCountedItems(100); // 只盘点100条

        when(orderRepository.selectById(1L)).thenReturn(order);

        // When & Then: 应抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            stocktakeService.finishOrder(1L);
        });

        assertTrue(exception.getMessage().contains("未盘点"));
    }

    // ========== TC-STK-035 取消盘点单 - 待盘点状态 ==========

    @Test
    @DisplayName("TC-STK-035: 取消盘点单-待盘点状态 - 成功取消")
    void testCancelStocktake_PendingStatus() {
        // Given: 待盘点状态的盘点单
        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_PENDING);

        when(orderRepository.selectById(1L)).thenReturn(order);

        // When: 取消盘点单
        stocktakeService.cancelOrder(1L, "取消原因");

        // Then: 状态变更为已取消
        ArgumentCaptor<StocktakeOrder> orderCaptor = ArgumentCaptor.forClass(StocktakeOrder.class);
        verify(orderRepository).updateById(orderCaptor.capture());
        assertEquals(StocktakeOrder.STATUS_CANCELLED, orderCaptor.getValue().getStatus());
        assertEquals("取消原因", orderCaptor.getValue().getRemark());
    }

    @Test
    @DisplayName("TC-STK-036: 取消盘点单-盘点中状态 - 应报错")
    void testCancelStocktake_CountingStatus() {
        // Given: 盘点中状态的盘点单
        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_COUNTING);

        when(orderRepository.selectById(1L)).thenReturn(order);

        // When & Then: 应抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            stocktakeService.cancelOrder(1L, "取消原因");
        });

        assertTrue(exception.getMessage().contains("待盘点"));
    }

    // ========== TC-STK-039 边界条件 - 实盘数量为0 ==========

    @Test
    @DisplayName("TC-STK-039: 边界条件-实盘数量为0 - 允许录入")
    void testCountItem_ZeroQty() {
        // Given: 系统数量50件，实盘0件
        StocktakeItem item = createTestStocktakeItem(1L, 1L, "SKU-009", 50);
        item.setStatus(StocktakeItem.STATUS_PENDING);

        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_COUNTING);

        when(itemRepository.selectById(1L)).thenReturn(item);
        when(orderRepository.selectById(1L)).thenReturn(order);
        when(itemRepository.countByOrderIdAndCounted(1L)).thenReturn(1);

        StocktakeCountDTO dto = new StocktakeCountDTO();
        dto.setItemId(1L);
        dto.setCountedQty(0); // 实盘0件
        dto.setDiffReason("loss");
        dto.setDiffRemark("全部破损");

        // When: 录入实盘数量0
        stocktakeService.countItem(dto, 1L, "admin");

        // Then: 差异为-50（全盘亏）
        ArgumentCaptor<StocktakeItem> itemCaptor = ArgumentCaptor.forClass(StocktakeItem.class);
        verify(itemRepository).updateById(itemCaptor.capture());
        assertEquals(0, itemCaptor.getValue().getCountedQty());
        assertEquals(-50, itemCaptor.getValue().getDiffQty());
    }

    // ========== TC-STK-040 边界条件 - 实盘数量超大 ==========

    @Test
    @DisplayName("TC-STK-040: 边界条件-实盘数量超大 - 允许录入")
    void testCountItem_LargeQty() {
        // Given: 系统数量50件，实盘999999件
        StocktakeItem item = createTestStocktakeItem(1L, 1L, "SKU-009", 50);
        item.setStatus(StocktakeItem.STATUS_PENDING);

        StocktakeOrder order = createTestStocktakeOrder(1L);
        order.setStatus(StocktakeOrder.STATUS_COUNTING);

        when(itemRepository.selectById(1L)).thenReturn(item);
        when(orderRepository.selectById(1L)).thenReturn(order);
        when(itemRepository.countByOrderIdAndCounted(1L)).thenReturn(1);

        StocktakeCountDTO dto = new StocktakeCountDTO();
        dto.setItemId(1L);
        dto.setCountedQty(999999);
        dto.setDiffReason("profit");

        // When: 录入超大数量
        stocktakeService.countItem(dto, 1L, "admin");

        // Then: 差异正确计算
        ArgumentCaptor<StocktakeItem> itemCaptor = ArgumentCaptor.forClass(StocktakeItem.class);
        verify(itemRepository).updateById(itemCaptor.capture());
        assertEquals(999999, itemCaptor.getValue().getCountedQty());
        assertEquals(999949, itemCaptor.getValue().getDiffQty());
    }

    // ========== 盘点单查询测试 ==========

    @Test
    @DisplayName("TC-STK-031: 查询盘点单列表 - 分页查询")
    void testQueryOrders_Pagination() {
        // Given: 查询参数
        StocktakeQueryDTO query = new StocktakeQueryDTO();
        query.setPage(1);
        query.setLimit(20);

        List<StocktakeOrder> orders = new ArrayList<>();
        orders.add(createTestStocktakeOrder(1L));
        orders.add(createTestStocktakeOrder(2L));

        Page<StocktakeOrder> page = new Page<>(1, 20, 2);
        page.setRecords(orders);

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // When: 查询
        Map<String, Object> result = stocktakeService.queryOrders(query);

        // Then: 返回分页结果
        assertNotNull(result);
        assertEquals(2L, result.get("total"));
        assertNotNull(result.get("list"));
    }

    @Test
    @DisplayName("TC-STK-031: 查询盘点单列表 - 按状态筛选")
    void testQueryOrders_ByStatus() {
        // Given: 按状态筛选
        StocktakeQueryDTO query = new StocktakeQueryDTO();
        query.setStatus(StocktakeOrder.STATUS_PENDING);

        Page<StocktakeOrder> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());

        when(orderRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // When: 查询
        Map<String, Object> result = stocktakeService.queryOrders(query);

        // Then: 返回结果
        assertNotNull(result);
        verify(orderRepository).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ========== 辅助方法 ==========

    private SysWarehouse createTestWarehouse(Long id, String code, String name) {
        SysWarehouse warehouse = new SysWarehouse();
        warehouse.setId(id);
        warehouse.setCode(code);
        warehouse.setName(name);
        return warehouse;
    }

    private List<Inventory> createTestInventories(int count) {
        List<Inventory> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Inventory inv = createTestInventory((long) i, "SKU-" + String.format("%03d", i),
                    "测试商品" + i, 100, "LOC-" + i);
            list.add(inv);
        }
        return list;
    }

    private List<Inventory> createTestInventoriesForZone(int count, Long zoneId) {
        List<Inventory> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Inventory inv = createTestInventory((long) i, "SKU-" + i, "测试商品" + i,
                    100, zoneId + "-LOC-" + i);
            list.add(inv);
        }
        return list;
    }

    private Inventory createTestInventory(Long productId, String skuCode, String productName,
            int qty, String locationCode) {
        Inventory inv = new Inventory();
        inv.setId(productId);
        inv.setProductId(productId);
        inv.setSkuCode(skuCode);
        inv.setProductName(productName);
        inv.setLocationCode(locationCode);
        inv.setQty(qty);
        inv.setAvailableQty(qty);
        inv.setLockedQty(0);
        inv.setWarehouseId(1L);
        inv.setWarehouseCode("WH-CN-001");
        inv.setBatchNo("IN20260527--" + skuCode);
        inv.setInboundTime(LocalDateTime.now());
        inv.setUpdateTime(LocalDateTime.now());
        return inv;
    }

    private List<BaseLocation> createTestLocations(Long zoneId, String zoneCode, int count) {
        List<BaseLocation> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BaseLocation loc = new BaseLocation();
            loc.setId((long) i);
            loc.setCode(zoneCode + "-LOC-" + i);
            loc.setZoneId(zoneId);
            loc.setZoneCode(zoneCode);
            list.add(loc);
        }
        return list;
    }

    private StocktakeOrder createTestStocktakeOrder(Long id) {
        StocktakeOrder order = new StocktakeOrder();
        order.setId(id);
        order.setOrderNo("ST20260527" + String.format("%04d", id));
        order.setWarehouseId(1L);
        order.setWarehouseCode("WH-CN-001");
        order.setWarehouseName("深圳总仓");
        order.setStocktakeType(StocktakeOrder.TYPE_FULL);
        order.setBlindMode(StocktakeOrder.BLIND_MODE_OFF);
        order.setStatus(StocktakeOrder.STATUS_PENDING);
        order.setTotalItems(156);
        order.setCountedItems(0);
        order.setPlanDate(LocalDate.now());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        return order;
    }

    private StocktakeItem createTestStocktakeItem(Long id, Long orderId, String skuCode, int systemQty) {
        StocktakeItem item = new StocktakeItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setOrderNo("ST20260527001");
        item.setSkuCode(skuCode);
        item.setProductName("测试商品");
        item.setLocationCode("A-R01-L01");
        item.setBatchNo("B001");
        item.setSystemQty(systemQty);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        return item;
    }
}
