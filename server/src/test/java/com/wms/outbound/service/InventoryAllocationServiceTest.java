package com.wms.outbound.service;

import com.wms.inbound.entity.Inventory;
import com.wms.outbound.dto.AllocationDTO;
import com.wms.outbound.dto.AllocationResultDTO;
import com.wms.inbound.repository.InventoryRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库存分配服务单元测试
 * TDD: 测试覆盖库存预占与锁定场景
 *
 * 测试用例对照：
 * - TC-INV-001: 库存锁定验证
 * - TC-INV-002: 库存扣减验证
 * - TC-INV-003: 库存释放验证
 * - TC-INV-004: 部分发货库存处理
 */
class InventoryAllocationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryAllocationService allocationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        allocationService = new InventoryAllocationService(inventoryRepository);
    }

    // ========== TC-INV-001 库存锁定验证 ==========

    @Test
    @DisplayName("TC-INV-001: 库存锁定验证 - 锁定后可用库存减少，锁定库存增加")
    void testLockInventory_Success() {
        // Given: 商品SKU-001当前库存100件，可用100件，锁定0件
        Long productId = 1L;
        Long locationId = 10L;
        Integer qtyToLock = 20;
        Long outboundOrderId = 1L;

        Inventory inventory = createTestInventory(productId, locationId, 100, 100, 0);

        when(inventoryRepository.selectList(any())).thenReturn(Arrays.asList(inventory));
        when(inventoryRepository.updateById(any(Inventory.class))).thenReturn(1);

        // When: 创建出库单并分配，锁定20件库存
        AllocationDTO dto = new AllocationDTO();
        dto.setProductId(productId);
        dto.setLocationId(locationId);
        dto.setQty(qtyToLock);
        dto.setOutboundOrderId(outboundOrderId);

        AllocationResultDTO result = allocationService.lockInventory(dto);

        // Then: 库存锁定成功
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(20, result.getAllocatedQty());
        assertEquals(80, result.getAvailableAfter());
        assertEquals(20, result.getLockedAfter());
    }

    @Test
    @DisplayName("TC-INV-001: 库存锁定 - 库存不足时锁定失败")
    void testLockInventory_InsufficientStock() {
        // Given: 商品SKU-001当前可用库存50件
        Long productId = 1L;
        Long locationId = 10L;
        Integer qtyToLock = 100; // 尝试锁定100件，超过可用库存

        Inventory inventory = createTestInventory(productId, locationId, 50, 50, 0);

        when(inventoryRepository.selectList(any())).thenReturn(Arrays.asList(inventory));

        // When: 锁定库存
        AllocationDTO dto = new AllocationDTO();
        dto.setProductId(productId);
        dto.setLocationId(locationId);
        dto.setQty(qtyToLock);

        AllocationResultDTO result = allocationService.lockInventory(dto);

        // Then: 锁定失败
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertTrue(result.getFailReason().contains("库存不足"));
    }

    // ========== TC-INV-002 库存扣减验证 ==========

    @Test
    @DisplayName("TC-INV-002: 库存扣减验证 - 发货后总库存和锁定库存同时减少")
    void testDeductInventory_Success() {
        // Given: SKU-001已锁定20件，准备发货20件
        Long productId = 1L;
        Long locationId = 10L;
        Integer qtyToDeduct = 20;
        Long outboundOrderId = 1L;

        Inventory inventory = createTestInventory(productId, locationId, 100, 80, 20);

        when(inventoryRepository.selectList(any())).thenReturn(Arrays.asList(inventory));
        when(inventoryRepository.updateById(any(Inventory.class))).thenReturn(1);

        // When: 发货确认，扣减库存
        boolean result = allocationService.deductInventory(outboundOrderId, productId, qtyToDeduct);

        // Then: 扣减成功
        assertTrue(result);
        verify(inventoryRepository).updateById(argThat(inv ->
            inv.getQty() == 80 && // 总库存减少20
            inv.getLockedQty() == 0 // 锁定库存减少20
        ));
    }

    // ========== TC-INV-003 库存释放验证 ==========

    @Test
    @DisplayName("TC-INV-003: 库存释放验证 - 取消出库单后锁定库存恢复为可用")
    void testReleaseInventory_Success() {
        // Given: SKU-001已锁定20件，未发货
        Long productId = 1L;
        Long locationId = 10L;
        Integer qtyToRelease = 20;
        Long outboundOrderId = 1L;

        Inventory inventory = createTestInventory(productId, locationId, 100, 80, 20);

        when(inventoryRepository.selectList(any())).thenReturn(Arrays.asList(inventory));
        when(inventoryRepository.updateById(any(Inventory.class))).thenReturn(1);

        // When: 取消出库单，释放库存
        boolean result = allocationService.releaseInventory(outboundOrderId, productId, qtyToRelease);

        // Then: 释放成功，锁定库存清零，可用库存恢复
        assertTrue(result);
        verify(inventoryRepository).updateById(argThat(inv ->
            inv.getLockedQty() == 0 && // 锁定清零
            inv.getAvailableQty() == 100 // 可用恢复
        ));
    }

    // ========== TC-INV-004 部分发货库存处理 ==========

    @Test
    @DisplayName("TC-INV-004: 部分发货库存处理 - 发货15件，释放5件锁定")
    void testPartialShipment() {
        // Given: 计划发货20件，实际发货15件
        Long productId = 1L;
        Long locationId = 10L;
        Integer shippedQty = 15;
        Integer remainingQty = 5;
        Long outboundOrderId = 1L;

        Inventory inventory = createTestInventory(productId, locationId, 100, 80, 20);

        when(inventoryRepository.selectList(any())).thenReturn(Arrays.asList(inventory));
        when(inventoryRepository.updateById(any(Inventory.class))).thenReturn(1);

        // When: 部分发货
        // 1. 扣减15件库存
        boolean deductResult = allocationService.deductInventory(outboundOrderId, productId, shippedQty);
        // 2. 重新查询库存状态（模拟扣减后的状态）
        Inventory afterDeduct = createTestInventory(productId, locationId, 85, 80, 5);
        when(inventoryRepository.selectList(any())).thenReturn(Arrays.asList(afterDeduct));
        // 3. 释放5件锁定
        boolean releaseResult = allocationService.releaseInventory(outboundOrderId, productId, remainingQty);

        // Then: 库存正确处理
        assertTrue(deductResult);
        assertTrue(releaseResult);
    }

    // ========== 辅助方法 ==========

    private Inventory createTestInventory(Long productId, Long locationId,
            Integer totalQty, Integer availableQty, Integer lockedQty) {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(productId);
        inventory.setLocationId(locationId);
        inventory.setLocationCode("A-R01-L01");
        inventory.setQty(totalQty);
        inventory.setAvailableQty(availableQty);
        inventory.setLockedQty(lockedQty);
        inventory.setBatchNo("B20260531001");
        inventory.setInboundTime(java.time.LocalDateTime.now());
        return inventory;
    }
}