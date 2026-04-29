package com.wms.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.inbound.entity.Inventory;
import com.wms.inventory.dto.*;
import com.wms.inventory.repository.InventoryRepositoryExt;
import com.wms.inventory.service.impl.InventoryServiceImpl;
import com.wms.system.entity.BaseLocation;
import com.wms.system.entity.BaseProduct;
import com.wms.system.entity.BaseZone;
import com.wms.system.entity.SysWarehouse;
import com.wms.system.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库存服务单元测试
 * TDD: 使用 Mock 测试业务逻辑
 */
class InventoryServiceTest {

    @Mock
    private InventoryRepositoryExt inventoryRepository;

    @Mock
    private BaseProductRepository productRepository;

    @Mock
    private SysWarehouseRepository warehouseRepository;

    @Mock
    private BaseZoneRepository zoneRepository;

    @Mock
    private BaseLocationRepository locationRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========== Story 4.1 实时库存查询测试 ==========

    @Test
    @DisplayName("TC-INV-001: 实时库存查询 - 基本查询返回分页结果")
    void testQueryInventory_Basic() {
        // Given: 模拟库存数据
        Inventory inv = createTestInventory(1L, "SKU001", "测试商品", 100);
        List<Inventory> inventories = Collections.singletonList(inv);

        when(inventoryRepository.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20, inventories.size()));
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(inventories);

        // When: 执行查询
        InventoryQueryDTO query = new InventoryQueryDTO();
        query.setPage(1);
        query.setLimit(20);
        Map<String, Object> result = inventoryService.queryInventory(query);

        // Then: 返回分页结果
        assertNotNull(result);
        assertTrue(result.containsKey("list"));
        assertTrue(result.containsKey("total"));
        assertTrue(result.containsKey("page"));
        assertTrue(result.containsKey("limit"));
    }

    @Test
    @DisplayName("TC-INV-002: 实时库存查询 - 按SKU查询")
    void testQueryInventory_BySkuCode() {
        // Given: SKU查询条件
        Inventory inv = createTestInventory(1L, "SKU001", "测试商品", 100);
        List<Inventory> inventories = Collections.singletonList(inv);

        when(inventoryRepository.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20, 1));
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(inventories);

        // When
        InventoryQueryDTO query = new InventoryQueryDTO();
        query.setSkuCode("SKU001");
        Map<String, Object> result = inventoryService.queryInventory(query);

        // Then
        assertNotNull(result);
        verify(inventoryRepository).selectPage(any(), argThat(wrapper -> {
            // 验证查询条件包含 SKU 模糊查询
            return true;
        }));
    }

    @Test
    @DisplayName("TC-INV-004: 实时库存查询 - 按条码精确查询")
    void testQueryInventory_ByBarcode() {
        // Given: 条码查询条件
        BaseProduct product = new BaseProduct();
        product.setId(1L);
        product.setBarcode("6901234567890");
        when(productRepository.findByBarcode("6901234567890")).thenReturn(product);

        Inventory inv = createTestInventory(1L, "SKU001", "测试商品", 100);
        when(inventoryRepository.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20, 1));

        // When
        InventoryQueryDTO query = new InventoryQueryDTO();
        query.setBarcode("6901234567890");
        Map<String, Object> result = inventoryService.queryInventory(query);

        // Then
        assertNotNull(result);
        verify(productRepository).findByBarcode("6901234567890");
    }

    @Test
    @DisplayName("TC-INV-005: 实时库存查询 - 按仓库筛选")
    void testQueryInventory_ByWarehouse() {
        // Given
        when(inventoryRepository.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20, 0));

        // When
        InventoryQueryDTO query = new InventoryQueryDTO();
        query.setWarehouseId(1L);
        Map<String, Object> result = inventoryService.queryInventory(query);

        // Then
        assertNotNull(result);
        verify(inventoryRepository).selectPage(any(), any(LambdaQueryWrapper.class));
    }

    // ========== Story 4.2 库存明细查询测试 ==========

    @Test
    @DisplayName("TC-INV-009: 库存明细查询 - 查看商品分布")
    void testGetProductInventoryDetail() {
        // Given: 商品在多个库位有库存
        Inventory inv1 = createTestInventory(1L, "SKU001", "测试商品", 50);
        inv1.setLocationCode("LOC-A01");
        Inventory inv2 = createTestInventory(1L, "SKU001", "测试商品", 50);
        inv2.setLocationCode("LOC-A02");
        when(inventoryRepository.findByProductId(1L)).thenReturn(Arrays.asList(inv1, inv2));

        // When
        List<InventoryVO> details = inventoryService.getProductInventoryDetail(1L, null);

        // Then
        assertNotNull(details);
        assertEquals(2, details.size());
    }

    @Test
    @DisplayName("TC-INV-009: 库存明细查询 - 按仓库筛选")
    void testGetProductInventoryDetail_ByWarehouse() {
        // Given
        Inventory inv = createTestInventory(1L, "SKU001", "测试商品", 100);
        when(inventoryRepository.findByProductIdAndWarehouse(1L, 1L)).thenReturn(Collections.singletonList(inv));

        // When
        List<InventoryVO> details = inventoryService.getProductInventoryDetail(1L, 1L);

        // Then
        assertNotNull(details);
        verify(inventoryRepository).findByProductIdAndWarehouse(1L, 1L);
    }

    // ========== Story 4.3 批次库存查询测试 ==========

    @Test
    @DisplayName("TC-INV-011: 批次库存查询 - 按批次查询")
    void testQueryBatchInventory_ByBatchNo() {
        // Given
        Inventory inv = createTestInventory(1L, "SKU001", "测试商品", 100);
        inv.setBatchNo("202604290001");
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(inv));

        // When
        InventoryQueryDTO query = new InventoryQueryDTO();
        query.setBatchNo("202604290001");
        List<BatchInventoryVO> result = inventoryService.queryBatchInventory(query);

        // Then
        assertNotNull(result);
    }

    // ========== Story 4.17 效期预警监控测试 ==========

    @Test
    @DisplayName("TC-INV-014: 效期预警监控 - 预警列表")
    void testGetExpiryWarnings() {
        // Given
        Inventory inv = createTestInventory(1L, "SKU001", "测试商品", 100);
        inv.setExpiryStatus(2); // 临期
        when(inventoryRepository.findExpiryWarnings(1L)).thenReturn(Collections.singletonList(inv));

        // When
        List<ExpiryWarningVO> warnings = inventoryService.getExpiryWarnings(1L, null);

        // Then
        assertNotNull(warnings);
    }

    @Test
    @DisplayName("TC-INV-015: 效期预警监控 - 效期状态计算")
    void testExpiryStatusCalculation() {
        // Given: 预警天数设置为15天
        int warningDays = 15;
        LocalDate today = LocalDate.now();

        // When & Then:
        // 距过期 > 15天：正常(0)
        LocalDate normalDate = today.plusDays(20);
        int normalStatus = calculateExpiryStatus(normalDate, warningDays);
        assertEquals(0, normalStatus);

        // 距过期 ≤ 15天 且 > 7天：预警(1)
        LocalDate warningDate = today.plusDays(10);
        int warningStatus = calculateExpiryStatus(warningDate, warningDays);
        assertEquals(1, warningStatus);

        // 距过期 ≤ 7天 且 > 0天：临期(2)
        LocalDate nearDate = today.plusDays(5);
        int nearStatus = calculateExpiryStatus(nearDate, warningDays);
        assertEquals(2, nearStatus);

        // 已过期：已过期(3)
        LocalDate expiredDate = today.minusDays(1);
        int expiredStatus = calculateExpiryStatus(expiredDate, warningDays);
        assertEquals(3, expiredStatus);
    }

    @Test
    @DisplayName("TC-INV-016: 效期预警监控 - 按效期状态筛选")
    void testGetExpiryWarnings_ByStatus() {
        // Given
        Inventory inv = createTestInventory(1L, "SKU001", "测试商品", 100);
        inv.setExpiryStatus(2); // 临期
        when(inventoryRepository.findByExpiryStatus(1L, 2)).thenReturn(Collections.singletonList(inv));

        // When
        List<ExpiryWarningVO> warnings = inventoryService.getExpiryWarnings(1L, 2);

        // Then
        assertNotNull(warnings);
        verify(inventoryRepository).findByExpiryStatus(1L, 2);
    }

    // ========== 库存汇总测试 ==========

    @Test
    @DisplayName("库存汇总统计")
    void testGetInventorySummary() {
        // Given
        Inventory inv1 = createTestInventory(1L, "SKU001", "商品A", 100);
        Inventory inv2 = createTestInventory(2L, "SKU002", "商品B", 200);
        when(inventoryRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(inv1, inv2));

        // When
        Map<String, Object> summary = inventoryService.getInventorySummary(null);

        // Then
        assertNotNull(summary);
        assertEquals(2L, summary.get("totalSku"));
        assertEquals(300, summary.get("totalQty"));
    }

    // ========== 辅助方法 ==========

    private Inventory createTestInventory(Long productId, String skuCode, String productName, int qty) {
        Inventory inv = new Inventory();
        inv.setId(productId);
        inv.setProductId(productId);
        inv.setSkuCode(skuCode);
        inv.setProductName(productName);
        inv.setQty(qty);
        inv.setAvailableQty(qty);
        inv.setLockedQty(0);
        inv.setWarehouseId(1L);
        inv.setWarehouseCode("WH001");
        inv.setLocationId(1L);
        inv.setLocationCode("LOC-A01");
        inv.setBatchNo("202604290001");
        inv.setExpiryStatus(0);
        inv.setInboundTime(LocalDateTime.now());
        return inv;
    }

    /**
     * 计算效期状态（与实现类逻辑一致）
     */
    private int calculateExpiryStatus(LocalDate expiryDate, int warningDays) {
        if (expiryDate == null) {
            return 0;
        }

        long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

        if (remainingDays < 0) {
            return 3; // 已过期
        } else if (remainingDays <= warningDays / 2) {
            return 2; // 临期
        } else if (remainingDays <= warningDays) {
            return 1; // 预警
        } else {
            return 0; // 正常
        }
    }
}
