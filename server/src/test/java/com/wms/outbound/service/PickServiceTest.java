package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.outbound.dto.*;
import com.wms.outbound.entity.*;
import com.wms.outbound.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 拣货服务单元测试
 * TDD: 测试覆盖拣货任务领取、扫码拣货、拣货完成场景
 *
 * 测试用例对照：
 * - TC-PICK-001: 领取拣货任务
 * - TC-PICK-002: 扫码拣货-正常流程
 * - TC-PICK-003: 扫码拣货-库位码错误
 * - TC-PICK-004: 扫码拣货-商品条码错误
 * - TC-PICK-005: 拣货数量差异
 * - TC-PICK-006: 拣货异常-库位缺货
 * - TC-PICK-007: 拣货异常-商品破损
 * - TC-PICK-008: 完成拣货
 * - TC-PICK-009: 部分拣货
 * - TC-PICK-010: 智能拣货路径验证
 */
class PickServiceTest {

    @Mock
    private PickTaskRepository pickTaskRepository;

    @Mock
    private PickRecordRepository pickRecordRepository;

    @Mock
    private OutboundOrderRepository orderRepository;

    @Mock
    private OutboundOrderItemRepository orderItemRepository;

    @Mock
    private WaveRepository waveRepository;

    @Mock
    private InventoryAllocationRepository allocationRepository;

    private PickService pickService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 手动初始化PickService，避免Lombok @RequiredArgsConstructor的问题
        pickService = new PickService(
            pickTaskRepository,
            pickRecordRepository,
            orderRepository,
            orderItemRepository,
            waveRepository,
            allocationRepository
        );
    }

    // ========== TC-PICK-001 领取拣货任务 ==========

    @Test
    @DisplayName("TC-PICK-001: 领取拣货任务 - 查看分配给自己的拣货任务列表")
    void testGetAssignedTasks() {
        // Given: 拣货员有分配给自己的任务
        Long userId = 100L;

        // 验证pickService已初始化
        assertNotNull(pickService, "PickService应已初始化");
        assertNotNull(pickTaskRepository, "PickTaskRepository应已初始化");

        List<PickTask> tasks = new ArrayList<>();
        PickTask task1 = createTestPickTask();
        task1.setPickUserId(userId);
        task1.setStatus(PickTask.STATUS_PENDING);
        tasks.add(task1);

        when(pickTaskRepository.selectPendingByUserId(userId)).thenReturn(tasks);
        when(pickRecordRepository.selectByWaveId(anyLong())).thenReturn(createTestPickRecords(3));

        // When: 查看任务列表
        List<PickTaskDetailDTO> result = pickService.getAssignedTasks(userId);

        // Then: 返回任务列表
        assertNotNull(result, "结果不应为null");
        assertFalse(result.isEmpty(), "结果不应为空");
        assertEquals(1, result.size(), "应返回1个任务");

        PickTaskDetailDTO taskDto = result.get(0);
        assertNotNull(taskDto, "任务DTO不应为null");
        assertEquals("PT20260531001", taskDto.getTaskNo(), "任务号应匹配");

        verify(pickTaskRepository).selectPendingByUserId(userId);
    }

    @Test
    @DisplayName("TC-PICK-001: 领取拣货任务 - 按最优路径排序拣货顺序")
    void testClaimTask_SortByOptimalPath() {
        // Given: 多个拣货项，分布在不同库位
        PickTask task = createTestPickTask();
        task.setStatus(PickTask.STATUS_PENDING);
        when(pickTaskRepository.selectById(1L)).thenReturn(task);

        // Mock: 不同库位的拣货记录（乱序）
        List<PickRecord> records = new ArrayList<>();
        PickRecord r1 = createTestPickRecord();
        r1.setLocationCode("B-R01-L01");
        r1.setSortOrder(0);
        records.add(r1);

        PickRecord r2 = createTestPickRecord();
        r2.setLocationCode("A-R01-L01");
        r2.setSortOrder(0);
        records.add(r2);

        PickRecord r3 = createTestPickRecord();
        r3.setLocationCode("A-R01-L02");
        r3.setSortOrder(0);
        records.add(r3);

        when(pickRecordRepository.selectByWaveId(anyLong())).thenReturn(records);
        when(pickTaskRepository.updateById(any(PickTask.class))).thenReturn(1);

        PickClaimDTO dto = new PickClaimDTO();
        dto.setPickUserId(100L);
        dto.setPickUserName("张三");

        // When: 领取任务
        PickTaskDetailDTO result = pickService.claimTask(1L, dto);

        // Then: 按库区顺序排列（A区→B区）
        assertNotNull(result);
        List<PickTaskDetailDTO.PickItemDTO> items = result.getPickItems();

        // 验证排序：A区应该在B区前面
        int aIndex = -1, bIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getLocationCode().startsWith("A")) {
                aIndex = i;
            }
            if (items.get(i).getLocationCode().startsWith("B")) {
                bIndex = i;
            }
        }
        assertTrue(aIndex < bIndex, "A区库位应该在B区库位前面");
    }

    @Test
    @DisplayName("TC-PICK-001: 可以放弃任务（返回任务池）")
    void testAbandonTask() {
        // Given: 进行中的任务
        Long taskId = 1L;
        PickTask task = createTestPickTask();
        task.setStatus(PickTask.STATUS_IN_PROGRESS);
        task.setPickUserId(100L);

        when(pickTaskRepository.selectById(taskId)).thenReturn(task);
        when(pickTaskRepository.updateById(any(PickTask.class))).thenReturn(1);

        // When: 放弃任务
        boolean result = pickService.abandonTask(taskId);

        // Then: 任务返回任务池
        assertTrue(result);
        verify(pickTaskRepository).updateById(argThat(t ->
            t.getStatus() == PickTask.STATUS_PENDING &&
            t.getPickUserId() == null
        ));
    }

    // ========== TC-PICK-002 扫码拣货 - 正常流程 ==========

    @Test
    @DisplayName("TC-PICK-002: 扫码拣货-正常流程 - 扫描库位码确认拣货位置")
    void testScanLocation_Success() {
        // Given: 拣货记录
        Long recordId = 1L;
        String locationCode = "A-R01-L01";

        PickRecord record = createTestPickRecord();
        record.setLocationCode(locationCode);
        record.setLocationScanned(PickRecord.SCAN_NO);
        when(pickRecordRepository.selectById(recordId)).thenReturn(record);
        when(pickRecordRepository.updateById(any(PickRecord.class))).thenReturn(1);

        // When: 扫描库位码
        boolean result = pickService.scanLocation(recordId, locationCode);

        // Then: 扫码成功
        assertTrue(result);
        verify(pickRecordRepository).updateById(argThat(r ->
            r.getLocationScanned() == PickRecord.SCAN_YES
        ));
    }

    @Test
    @DisplayName("TC-PICK-002: 扫码拣货 - 扫描商品条码确认商品")
    void testScanProduct_Success() {
        // Given: 拣货记录
        Long recordId = 1L;
        String barcode = "SKU001-BARCODE";

        PickRecord record = createTestPickRecord();
        record.setBarcode(barcode);
        record.setProductScanned(PickRecord.SCAN_NO);
        record.setLocationScanned(PickRecord.SCAN_YES); // 库位已扫码
        when(pickRecordRepository.selectById(recordId)).thenReturn(record);
        when(pickRecordRepository.updateById(any(PickRecord.class))).thenReturn(1);

        // When: 扫描商品条码
        boolean result = pickService.scanProduct(recordId, barcode);

        // Then: 扫码成功
        assertTrue(result);
        verify(pickRecordRepository).updateById(argThat(r ->
            r.getProductScanned() == PickRecord.SCAN_YES
        ));
    }

    @Test
    @DisplayName("TC-PICK-002: 扫码拣货 - 录入实际拣货数量")
    void testConfirmPick_EnterActualQty() {
        // Given: 拣货记录，计划拣货10件
        Long recordId = 1L;
        PickRecord record = createTestPickRecord();
        record.setPlanQty(10);
        record.setLocationScanned(PickRecord.SCAN_YES);
        record.setProductScanned(PickRecord.SCAN_YES);
        record.setStatus(PickRecord.STATUS_PICKING);

        when(pickRecordRepository.selectById(recordId)).thenReturn(record);
        when(pickRecordRepository.updateById(any(PickRecord.class))).thenReturn(1);
        when(orderItemRepository.selectById(anyLong())).thenReturn(new OutboundOrderItem());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PickConfirmDTO dto = new PickConfirmDTO();
        dto.setRecordId(recordId);
        dto.setActualQty(10);

        // When: 录入实际拣货数量
        PickCompleteResultDTO result = pickService.confirmPick(dto);

        // Then: 拣货成功
        assertTrue(result.getSuccess());
        verify(pickRecordRepository).updateById(argThat(r ->
            r.getActualQty() == 10 &&
            r.getDiffQty() == 0 &&
            r.getStatus() == PickRecord.STATUS_COMPLETED
        ));
    }

    // ========== TC-PICK-003 扫码拣货 - 库位码错误 ==========

    @Test
    @DisplayName("TC-PICK-003: 扫码拣货-库位码错误 - 提示正确库位")
    void testScanLocation_WrongLocation() {
        // Given: 拣货记录，应拣库位A-R01-L01
        Long recordId = 1L;
        String correctLocation = "A-R01-L01";
        String wrongLocation = "B-R01-L01";

        PickRecord record = createTestPickRecord();
        record.setLocationCode(correctLocation);
        when(pickRecordRepository.selectById(recordId)).thenReturn(record);

        // When: 扫描错误库位码
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> pickService.scanLocation(recordId, wrongLocation)
        );

        // Then: 提示错误
        assertTrue(exception.getMessage().contains("库位不匹配"));
        assertTrue(exception.getMessage().contains(correctLocation));
    }

    // ========== TC-PICK-004 扫码拣货 - 商品条码错误 ==========

    @Test
    @DisplayName("TC-PICK-004: 扫码拣货-商品条码错误 - 提示正确商品")
    void testScanProduct_WrongProduct() {
        // Given: 拣货记录，应拣商品SKU001
        Long recordId = 1L;
        String correctBarcode = "SKU001-BARCODE";
        String wrongBarcode = "SKU002-BARCODE";

        PickRecord record = createTestPickRecord();
        record.setBarcode(correctBarcode);
        record.setLocationScanned(PickRecord.SCAN_YES);
        when(pickRecordRepository.selectById(recordId)).thenReturn(record);

        // When: 扫描错误商品条码
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> pickService.scanProduct(recordId, wrongBarcode)
        );

        // Then: 提示错误
        assertTrue(exception.getMessage().contains("商品不匹配"));
    }

    // ========== TC-PICK-005 拣货数量差异 ==========

    @Test
    @DisplayName("TC-PICK-005: 拣货数量差异 - 提示确认并填写差异原因")
    void testConfirmPick_QtyDifference() {
        // Given: 计划拣货10件，实际拣货8件
        Long recordId = 1L;
        PickRecord record = createTestPickRecord();
        record.setPlanQty(10);
        record.setLocationScanned(PickRecord.SCAN_YES);
        record.setProductScanned(PickRecord.SCAN_YES);
        record.setStatus(PickRecord.STATUS_PICKING);

        when(pickRecordRepository.selectById(recordId)).thenReturn(record);
        when(pickRecordRepository.updateById(any(PickRecord.class))).thenReturn(1);
        when(orderItemRepository.selectById(anyLong())).thenReturn(new OutboundOrderItem());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PickConfirmDTO dto = new PickConfirmDTO();
        dto.setRecordId(recordId);
        dto.setActualQty(8);
        dto.setDiffReason("库位实际库存不足");

        // When: 确认拣货
        PickCompleteResultDTO result = pickService.confirmPick(dto);

        // Then: 记录差异
        assertTrue(result.getSuccess());
        verify(pickRecordRepository).updateById(argThat(r ->
            r.getActualQty() == 8 &&
            r.getDiffQty() == 2 &&
            r.getDiffReason() != null
        ));
    }

    // ========== TC-PICK-006 拣货异常 - 库位缺货 ==========

    @Test
    @DisplayName("TC-PICK-006: 拣货异常-库位缺货 - 标记异常并记录实际数量")
    void testReportException_LocationShortage() {
        // Given: 库位实际库存不足
        Long recordId = 1L;
        PickRecord record = createTestPickRecord();
        record.setPlanQty(10);
        record.setStatus(PickRecord.STATUS_PICKING);
        record.setLocationScanned(PickRecord.SCAN_YES);  // 已扫码
        record.setProductScanned(PickRecord.SCAN_YES);   // 已扫码

        when(pickRecordRepository.selectById(recordId)).thenReturn(record);
        when(pickRecordRepository.updateById(any(PickRecord.class))).thenReturn(1);
        when(orderItemRepository.selectById(anyLong())).thenReturn(new OutboundOrderItem());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PickConfirmDTO dto = new PickConfirmDTO();
        dto.setRecordId(recordId);
        dto.setIsException(true);
        dto.setExceptionType(PickRecord.EXCEPTION_SHORTAGE);
        dto.setExceptionQty(5);
        dto.setActualQty(5);
        dto.setExceptionRemark("库位仅有5件，缺货5件");

        // When: 标记异常
        PickCompleteResultDTO result = pickService.confirmPick(dto);

        // Then: 记录异常信息
        assertTrue(result.getSuccess());
        verify(pickRecordRepository).updateById(argThat(r ->
            r.getIsException() == 1 &&
            r.getExceptionType() == PickRecord.EXCEPTION_SHORTAGE &&
            r.getExceptionQty() == 5 &&
            r.getStatus() == PickRecord.STATUS_EXCEPTION
        ));
    }

    // ========== TC-PICK-007 拣货异常 - 商品破损 ==========

    @Test
    @DisplayName("TC-PICK-007: 拣货异常-商品破损 - 标记破损并移至残次品区")
    void testReportException_Damaged() {
        // Given: 发现商品破损
        Long recordId = 1L;
        PickRecord record = createTestPickRecord();
        record.setPlanQty(10);
        record.setStatus(PickRecord.STATUS_PICKING);
        record.setLocationScanned(PickRecord.SCAN_YES);  // 已扫码
        record.setProductScanned(PickRecord.SCAN_YES);   // 已扫码

        when(pickRecordRepository.selectById(recordId)).thenReturn(record);
        when(pickRecordRepository.updateById(any(PickRecord.class))).thenReturn(1);
        when(orderItemRepository.selectById(anyLong())).thenReturn(new OutboundOrderItem());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PickConfirmDTO dto = new PickConfirmDTO();
        dto.setRecordId(recordId);
        dto.setIsException(true);
        dto.setExceptionType(PickRecord.EXCEPTION_DAMAGED);
        dto.setExceptionQty(2);
        dto.setActualQty(8);
        dto.setExceptionRemark("发现2件商品破损");

        // When: 标记破损
        PickCompleteResultDTO result = pickService.confirmPick(dto);

        // Then: 记录破损信息
        assertTrue(result.getSuccess());
        verify(pickRecordRepository).updateById(argThat(r ->
            r.getExceptionType() == PickRecord.EXCEPTION_DAMAGED &&
            r.getExceptionQty() == 2
        ));
    }

    // ========== TC-PICK-008 完成拣货 ==========

    @Test
    @DisplayName("TC-PICK-008: 完成拣货 - 所有商品拣货完成后提交")
    void testCompletePick_AllItemsDone() {
        // Given: 所有拣货项已完成
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PICKING);

        when(orderRepository.selectById(orderId)).thenReturn(order);
        when(pickRecordRepository.selectByOrderId(orderId)).thenReturn(createCompletedPickRecords());
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(pickRecordRepository.sumPickedQtyByWaveId(anyLong())).thenReturn(100);

        // When: 提交拣货完成
        PickCompleteResultDTO result = pickService.completeOrderPick(orderId);

        // Then: 订单状态变为待打包
        assertTrue(result.getSuccess());
        assertEquals(OutboundOrder.STATUS_PACKING, result.getNewStatus());
        verify(orderRepository).updateById(argThat(o ->
            o.getStatus() == OutboundOrder.STATUS_PACKING
        ));
    }

    @Test
    @DisplayName("TC-PICK-008: 完成拣货 - 系统校验拣货数量与应出数量是否一致")
    void testCompletePick_VerifyQty() {
        // Given: 拣货数量与计划数量一致
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PICKING);
        order.setTotalQty(100);

        when(orderRepository.selectById(orderId)).thenReturn(order);

        // Mock: 已完成的拣货记录，总数量100
        List<PickRecord> records = createCompletedPickRecords();
        when(pickRecordRepository.selectByOrderId(orderId)).thenReturn(records);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);
        when(pickRecordRepository.sumPickedQtyByWaveId(anyLong())).thenReturn(100);

        // When: 提交拣货完成
        PickCompleteResultDTO result = pickService.completeOrderPick(orderId);

        // Then: 校验通过，无差异
        assertTrue(result.getSuccess());
        assertFalse(result.getHasDifference());
    }

    @Test
    @DisplayName("TC-PICK-008: 完成拣货 - 不一致时显示差异明细")
    void testCompletePick_ShowDifferences() {
        // Given: 拣货数量与计划数量不一致
        Long orderId = 1L;
        OutboundOrder order = createTestOrder();
        order.setStatus(OutboundOrder.STATUS_PICKING);
        order.setTotalQty(100);

        when(orderRepository.selectById(orderId)).thenReturn(order);

        // Mock: 已完成的拣货记录，总数量90（差异10）
        List<PickRecord> records = createPickRecordsWithDiff();
        when(pickRecordRepository.selectByOrderId(orderId)).thenReturn(records);
        when(orderRepository.updateById(any(OutboundOrder.class))).thenReturn(1);

        // When: 提交拣货完成
        PickCompleteResultDTO result = pickService.completeOrderPick(orderId);

        // Then: 显示差异明细
        assertTrue(result.getSuccess());
        assertTrue(result.getHasDifference());
        assertNotNull(result.getDifferenceList());
        assertTrue(result.getDifferenceList().size() > 0);
    }

    // ========== TC-PICK-009 部分拣货 ==========

    @Test
    @DisplayName("TC-PICK-009: 部分拣货 - 支持分批拣货场景")
    void testPartialPick() {
        // Given: 计划拣货20件，本次拣货10件
        Long recordId = 1L;
        PickRecord record = createTestPickRecord();
        record.setPlanQty(20);
        record.setActualQty(0);
        record.setStatus(PickRecord.STATUS_PICKING);
        record.setLocationScanned(PickRecord.SCAN_YES);  // 已扫码
        record.setProductScanned(PickRecord.SCAN_YES);   // 已扫码

        when(pickRecordRepository.selectById(recordId)).thenReturn(record);
        when(pickRecordRepository.updateById(any(PickRecord.class))).thenReturn(1);
        when(orderItemRepository.selectById(anyLong())).thenReturn(new OutboundOrderItem());
        when(orderItemRepository.updateById(any(OutboundOrderItem.class))).thenReturn(1);

        PickConfirmDTO dto = new PickConfirmDTO();
        dto.setRecordId(recordId);
        dto.setActualQty(10); // 本次只拣10件

        // When: 部分拣货确认
        PickCompleteResultDTO result = pickService.confirmPick(dto);

        // Then: 记录已拣数量，状态保持拣货中
        assertTrue(result.getSuccess());
        verify(pickRecordRepository).updateById(argThat(r ->
            r.getActualQty() == 10 &&
            r.getStatus() == PickRecord.STATUS_PICKING // 仍为拣货中
        ));
    }

    // ========== TC-PICK-010 智能拣货路径验证 ==========

    @Test
    @DisplayName("TC-PICK-010: 智能拣货路径验证 - 按库区顺序排列")
    void testOptimalPath_ByZone() {
        // Given: 拣货任务含多个库区的库位
        PickTask task = createTestPickTask();
        when(pickTaskRepository.selectById(1L)).thenReturn(task);

        // Mock: 不同库区的拣货记录
        List<PickRecord> records = new ArrayList<>();
        String[] locations = {"B-R01-L01", "A-R02-L03", "A-R01-L01", "C-R01-L02"};
        for (int i = 0; i < locations.length; i++) {
            PickRecord r = createTestPickRecord();
            r.setLocationCode(locations[i]);
            records.add(r);
        }
        when(pickRecordRepository.selectByWaveId(anyLong())).thenReturn(records);
        when(pickTaskRepository.updateById(any(PickTask.class))).thenReturn(1);

        PickClaimDTO dto = new PickClaimDTO();
        dto.setPickUserId(100L);

        // When: 领取任务并获取拣货清单
        PickTaskDetailDTO result = pickService.claimTask(1L, dto);

        // Then: 按库区顺序排列（A→B→C）
        List<PickTaskDetailDTO.PickItemDTO> items = result.getPickItems();
        String prevZone = "";
        for (PickTaskDetailDTO.PickItemDTO item : items) {
            String currentZone = item.getLocationCode().substring(0, 1);
            assertTrue(currentZone.compareTo(prevZone) >= 0,
                "库区顺序应为 A→B→C，实际: " + prevZone + " → " + currentZone);
            prevZone = currentZone;
        }
    }

    @Test
    @DisplayName("TC-PICK-010: 智能拣货路径验证 - 同库区内按巷道排序")
    void testOptimalPath_ByAisle() {
        // Given: 同一库区的多个库位
        List<PickRecord> records = new ArrayList<>();
        String[] locations = {"A-R03-L01", "A-R01-L02", "A-R02-L01", "A-R01-L01"};
        for (String loc : locations) {
            PickRecord r = createTestPickRecord();
            r.setLocationCode(loc);
            records.add(r);
        }

        // When: 排序拣货路径
        List<PickRecord> sorted = pickService.sortPickPath(records);

        // Then: 按巷道号排序（R01→R02→R03）
        for (int i = 1; i < sorted.size(); i++) {
            String prevAisle = extractAisle(sorted.get(i - 1).getLocationCode());
            String currAisle = extractAisle(sorted.get(i).getLocationCode());
            assertTrue(prevAisle.compareTo(currAisle) <= 0,
                "巷道顺序应为 R01→R02→R03");
        }
    }

    // ========== 边界值测试 ==========

    @Test
    @DisplayName("边界值测试: 拣货数量超过计划数量 - 不允许")
    void testConfirmPick_ExceedPlanQty() {
        // Given: 计划拣货10件
        Long recordId = 1L;
        PickRecord record = createTestPickRecord();
        record.setPlanQty(10);
        record.setStatus(PickRecord.STATUS_PICKING);
        record.setLocationScanned(PickRecord.SCAN_YES);
        record.setProductScanned(PickRecord.SCAN_YES);

        when(pickRecordRepository.selectById(recordId)).thenReturn(record);

        PickConfirmDTO dto = new PickConfirmDTO();
        dto.setRecordId(recordId);
        dto.setActualQty(15); // 超过计划数量

        // When & Then: 抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> pickService.confirmPick(dto)
        );
        assertTrue(exception.getMessage().contains("拣货数量不能超过计划数量"));
    }

    @Test
    @DisplayName("边界值测试: 重复扫描库位 - 提示已完成")
    void testScanLocation_AlreadyScanned() {
        // Given: 库位已扫码
        Long recordId = 1L;
        PickRecord record = createTestPickRecord();
        record.setLocationScanned(PickRecord.SCAN_YES);

        when(pickRecordRepository.selectById(recordId)).thenReturn(record);

        // When: 再次扫描库位
        boolean result = pickService.scanLocation(recordId, record.getLocationCode());

        // Then: 返回true但不更新（幂等性）
        assertTrue(result);
        verify(pickRecordRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("边界值测试: 拣货数量为0 - 必须标记异常")
    void testConfirmPick_ZeroQtyMustMarkException() {
        // Given: 实际拣货数量为0
        Long recordId = 1L;
        PickRecord record = createTestPickRecord();
        record.setPlanQty(10);
        record.setStatus(PickRecord.STATUS_PICKING);
        record.setLocationScanned(PickRecord.SCAN_YES);
        record.setProductScanned(PickRecord.SCAN_YES);

        when(pickRecordRepository.selectById(recordId)).thenReturn(record);

        PickConfirmDTO dto = new PickConfirmDTO();
        dto.setRecordId(recordId);
        dto.setActualQty(0);
        // 未标记异常

        // When & Then: 要求标记异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> pickService.confirmPick(dto)
        );
        assertTrue(exception.getMessage().contains("必须标记异常"));
    }

    // ========== 辅助方法 ==========

    private PickTask createTestPickTask() {
        PickTask task = new PickTask();
        task.setId(1L);
        task.setTaskNo("PT20260531001");
        task.setWaveId(1L);
        task.setWaveNo("W20260531001");
        task.setStatus(PickTask.STATUS_PENDING);
        task.setTotalItems(5);
        task.setTotalQty(50);
        return task;
    }

    private PickRecord createTestPickRecord() {
        PickRecord record = new PickRecord();
        record.setId(1L);
        record.setOutboundOrderId(1L);
        record.setOutboundOrderNo("OB20260531001");
        record.setOutboundItemId(1L);
        record.setWaveId(1L);
        record.setProductId(100L);
        record.setSkuCode("SKU001");
        record.setProductName("测试商品");
        record.setBarcode("SKU001-BARCODE");
        record.setWarehouseId(1L);
        record.setLocationId(10L);
        record.setLocationCode("A-R01-L01");
        record.setPlanQty(10);
        record.setActualQty(0);
        record.setDiffQty(0);
        record.setLocationScanned(PickRecord.SCAN_NO);
        record.setProductScanned(PickRecord.SCAN_NO);
        record.setStatus(PickRecord.STATUS_PENDING);
        record.setIsException(0);
        record.setSortOrder(0);
        return record;
    }

    private List<PickRecord> createTestPickRecords(int count) {
        List<PickRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PickRecord record = createTestPickRecord();
            record.setId((long) (i + 1));
            record.setLocationCode("A-R0" + (i + 1) + "-L01");
            record.setSortOrder(i);
            records.add(record);
        }
        return records;
    }

    private List<PickRecord> createCompletedPickRecords() {
        List<PickRecord> records = createTestPickRecords(5);
        records.forEach(r -> {
            r.setStatus(PickRecord.STATUS_COMPLETED);
            r.setActualQty(r.getPlanQty());
        });
        return records;
    }

    private List<PickRecord> createPickRecordsWithDiff() {
        List<PickRecord> records = createTestPickRecords(5);
        // 前3个完成，后2个有差异
        for (int i = 0; i < 3; i++) {
            records.get(i).setStatus(PickRecord.STATUS_COMPLETED);
            records.get(i).setActualQty(records.get(i).getPlanQty());
        }
        for (int i = 3; i < 5; i++) {
            records.get(i).setStatus(PickRecord.STATUS_COMPLETED);
            records.get(i).setActualQty(records.get(i).getPlanQty() - 1); // 差异1件
            records.get(i).setDiffQty(1);
        }
        return records;
    }

    private OutboundOrder createTestOrder() {
        OutboundOrder order = new OutboundOrder();
        order.setId(1L);
        order.setOrderNo("OB20260531001");
        order.setStatus(OutboundOrder.STATUS_PICKING);
        order.setTotalQty(100);
        order.setWaveId(1L);
        return order;
    }

    private String extractAisle(String locationCode) {
        // 从库位编码中提取巷道号，如 A-R01-L01 -> R01
        String[] parts = locationCode.split("-");
        return parts.length >= 2 ? parts[1] : "";
    }
}