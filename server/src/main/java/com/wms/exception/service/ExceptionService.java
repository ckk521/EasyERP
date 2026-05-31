package com.wms.exception.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.exception.dto.ExceptionOrderCreateDTO;
import com.wms.exception.dto.HandleDTO;
import com.wms.exception.dto.IsolateDTO;
import com.wms.exception.entity.ExceptionItem;
import com.wms.exception.entity.ExceptionOrder;
import com.wms.exception.repository.ExceptionItemRepository;
import com.wms.exception.repository.ExceptionOrderRepository;
import com.wms.inbound.entity.InspectRecord;
import com.wms.inbound.entity.Inventory;
import com.wms.inbound.entity.InventoryTransaction;
import com.wms.inbound.entity.InboundOrder;
import com.wms.inbound.entity.InboundOrderItem;
import com.wms.inbound.entity.ReceiveRecord;
import com.wms.inbound.repository.InspectRecordRepository;
import com.wms.inbound.repository.InventoryRepository;
import com.wms.inbound.repository.InventoryTransactionRepository;
import com.wms.inbound.repository.InboundOrderRepository;
import com.wms.inbound.repository.InboundOrderItemRepository;
import com.wms.inbound.repository.PutawayRecordRepository;
import com.wms.inbound.repository.ReceiveRecordRepository;
import com.wms.inbound.service.InboundStatusService;
import com.wms.system.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionService {

    private final ExceptionOrderRepository orderRepository;
    private final ExceptionItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InspectRecordRepository inspectRecordRepository;
    private final ReceiveRecordRepository receiveRecordRepository;
    private final PutawayRecordRepository putawayRecordRepository;
    private final InboundStatusService inboundStatusService;

    /**
     * 创建异常处理单
     */
    @Transactional
    public Map<String, Object> createExceptionOrder(ExceptionOrderCreateDTO dto, Long userId, String userName) {
        // 生成异常处理单号
        String orderNo = generateOrderNo();

        // 创建主单
        ExceptionOrder order = new ExceptionOrder();
        order.setOrderNo(orderNo);
        order.setInboundOrderId(dto.getInboundOrderId());
        order.setInboundOrderNo(dto.getInboundOrderNo());
        order.setPurchaseOrderId(dto.getPurchaseOrderId());
        order.setPurchaseOrderNo(dto.getPurchaseOrderNo());
        order.setSupplierId(dto.getSupplierId());
        order.setSupplierCode(dto.getSupplierCode());
        order.setSupplierName(dto.getSupplierName());
        order.setWarehouseId(dto.getWarehouseId());
        order.setWarehouseCode(dto.getWarehouseCode());
        order.setZoneId(dto.getZoneId());
        order.setZoneCode(dto.getZoneCode());
        order.setExceptionType(dto.getExceptionType());
        order.setExceptionReason(dto.getExceptionReason());
        order.setSourceType(dto.getSourceType());
        order.setRemark(dto.getRemark());
        order.setStatus(ExceptionOrder.STATUS_PENDING);
        order.setCreateUserId(userId);
        order.setCreateUserName(userName);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 计算总数量
        int totalQty = dto.getItems().stream()
                .mapToInt(ExceptionOrderCreateDTO.ExceptionItemDTO::getExceptionQty)
                .sum();
        order.setTotalQty(totalQty);

        orderRepository.insert(order);

        // 创建明细
        for (ExceptionOrderCreateDTO.ExceptionItemDTO itemDto : dto.getItems()) {
            ExceptionItem item = new ExceptionItem();
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setProductId(itemDto.getProductId());
            item.setSkuCode(itemDto.getSkuCode());
            item.setProductName(itemDto.getProductName());
            item.setBarcode(itemDto.getBarcode());
            item.setBatchNo(itemDto.getBatchNo());
            item.setExceptionQty(itemDto.getExceptionQty());
            item.setExceptionType(itemDto.getExceptionType() != null ? itemDto.getExceptionType() : dto.getExceptionType());
            item.setExceptionReason(itemDto.getExceptionReason());
            item.setInboundItemId(itemDto.getInboundItemId());
            item.setStatus(ExceptionItem.STATUS_PENDING);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());

            itemRepository.insert(item);
        }

        return toOrderMap(order);
    }

    /**
     * 生成异常处理单号 EX+年月日+4位序号
     */
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "EX" + dateStr;

        LambdaQueryWrapper<ExceptionOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(ExceptionOrder::getOrderNo, prefix)
                .orderByDesc(ExceptionOrder::getOrderNo)
                .last("LIMIT 1");

        ExceptionOrder lastOrder = orderRepository.selectOne(wrapper);

        int seq = 1;
        if (lastOrder != null && lastOrder.getOrderNo() != null) {
            String lastNo = lastOrder.getOrderNo();
            if (lastNo.startsWith(prefix)) {
                seq = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
            }
        }

        return prefix + String.format("%04d", seq);
    }

    /**
     * 查询异常处理单列表
     */
    public Map<String, Object> listExceptionOrders(PageDTO pageDTO, String keyword, Long inboundOrderId,
            Long supplierId, Integer exceptionType, Integer status, String startTime, String endTime) {
        LambdaQueryWrapper<ExceptionOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ExceptionOrder::getOrderNo, keyword)
                    .or().like(ExceptionOrder::getInboundOrderNo, keyword));
        }
        if (inboundOrderId != null) {
            wrapper.eq(ExceptionOrder::getInboundOrderId, inboundOrderId);
        }
        if (supplierId != null) {
            wrapper.eq(ExceptionOrder::getSupplierId, supplierId);
        }
        if (exceptionType != null) {
            wrapper.eq(ExceptionOrder::getExceptionType, exceptionType);
        }
        if (status != null) {
            wrapper.eq(ExceptionOrder::getStatus, status);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(ExceptionOrder::getCreateTime, LocalDateTime.parse(startTime + "T00:00:00"));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(ExceptionOrder::getCreateTime, LocalDateTime.parse(endTime + "T23:59:59"));
        }

        wrapper.orderByDesc(ExceptionOrder::getCreateTime);

        IPage<ExceptionOrder> page = orderRepository.selectPage(
                new Page<>(pageDTO.getPage(), pageDTO.getLimit()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream()
                .map(this::toOrderMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    /**
     * 查询异常处理单详情
     */
    public Map<String, Object> getExceptionOrderById(Long id) {
        ExceptionOrder order = orderRepository.selectById(id);
        if (order == null) {
            throw new RuntimeException("异常处理单不存在");
        }

        Map<String, Object> result = toOrderMap(order);

        // 查询明细
        LambdaQueryWrapper<ExceptionItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExceptionItem::getOrderId, id);
        List<ExceptionItem> items = itemRepository.selectList(wrapper);

        List<Map<String, Object>> itemMaps = items.stream()
                .map(this::toItemMap)
                .collect(Collectors.toList());

        result.put("items", itemMaps);
        return result;
    }

    /**
     * 隔离入库
     */
    @Transactional
    public Map<String, Object> isolate(IsolateDTO dto, Long userId, String userName) {
        ExceptionOrder order = orderRepository.selectById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("异常处理单不存在");
        }

        if (order.getStatus() != ExceptionOrder.STATUS_PENDING) {
            throw new RuntimeException("只有待处理状态的异常单可以执行隔离入库");
        }

        // 更新明细的库位信息
        for (IsolateDTO.ItemLocationDTO itemDto : dto.getItems()) {
            ExceptionItem item = itemRepository.selectById(itemDto.getItemId());
            if (item == null) {
                throw new RuntimeException("异常明细不存在");
            }

            item.setLocationId(itemDto.getLocationId());
            item.setLocationCode(itemDto.getLocationCode());
            item.setStatus(ExceptionItem.STATUS_ISOLATED);
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.updateById(item);

            // 增加隔离库存
            Inventory inventory = new Inventory();
            inventory.setWarehouseId(order.getWarehouseId());
            inventory.setWarehouseCode(order.getWarehouseCode());
            inventory.setLocationId(itemDto.getLocationId());
            inventory.setLocationCode(itemDto.getLocationCode());
            inventory.setProductId(item.getProductId());
            inventory.setSkuCode(item.getSkuCode());
            inventory.setProductName(item.getProductName());
            inventory.setBatchNo(item.getBatchNo());
            inventory.setQty(item.getExceptionQty());
            inventory.setAvailableQty(0); // 隔离库存不可用
            inventory.setLockedQty(item.getExceptionQty());
            inventory.setInboundOrderId(order.getInboundOrderId());
            inventory.setInboundOrderNo(order.getOrderNo());
            inventory.setInboundTime(LocalDateTime.now());
            inventory.setCreateTime(LocalDateTime.now());
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryRepository.insert(inventory);

            // 记录库存事务
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setTransactionNo(generateTransactionNo());
            transaction.setTransactionType(InventoryTransaction.TYPE_INBOUND);
            transaction.setWarehouseId(order.getWarehouseId());
            transaction.setLocationId(itemDto.getLocationId());
            transaction.setLocationCode(itemDto.getLocationCode());
            transaction.setProductId(item.getProductId());
            transaction.setSkuCode(item.getSkuCode());
            transaction.setBatchNo(item.getBatchNo());
            transaction.setQtyChange(item.getExceptionQty());
            transaction.setQtyBefore(0);
            transaction.setQtyAfter(item.getExceptionQty());
            transaction.setRefOrderType("EXCEPTION");
            transaction.setRefOrderId(order.getId());
            transaction.setRefOrderNo(order.getOrderNo());
            transaction.setRemark("异常隔离入库");
            transaction.setCreateUser(userId);
            transaction.setCreateTime(LocalDateTime.now());
            transactionRepository.insert(transaction);
        }

        // 更新主单状态
        order.setStatus(ExceptionOrder.STATUS_PROCESSING);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        return getExceptionOrderById(order.getId());
    }

    /**
     * 撤销隔离入库
     *
     * 业务逻辑：
     * 1. 扣减隔离库存
     * 2. 清除明细的库位信息
     * 3. 主单状态回滚为"待处理"
     */
    @Transactional
    public Map<String, Object> undoIsolate(Long orderId, Long userId, String userName) {
        ExceptionOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("异常处理单不存在");
        }

        if (order.getStatus() != ExceptionOrder.STATUS_PROCESSING) {
            throw new RuntimeException("只有处理中状态的异常单可以撤销隔离入库");
        }

        // 查询异常明细
        LambdaQueryWrapper<ExceptionItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(ExceptionItem::getOrderId, orderId);
        List<ExceptionItem> items = itemRepository.selectList(itemWrapper);

        // 扣减隔离库存
        for (ExceptionItem item : items) {
            if (item.getStatus() == ExceptionItem.STATUS_ISOLATED && item.getLocationId() != null) {
                // 查找隔离库存
                LambdaQueryWrapper<Inventory> invWrapper = new LambdaQueryWrapper<>();
                invWrapper.eq(Inventory::getLocationId, item.getLocationId())
                         .eq(Inventory::getProductId, item.getProductId())
                         .gt(Inventory::getLockedQty, 0);
                Inventory inventory = inventoryRepository.selectOne(invWrapper);

                if (inventory != null) {
                    int newQty = inventory.getQty() - item.getExceptionQty();
                    int newLockedQty = inventory.getLockedQty() - item.getExceptionQty();

                    if (newQty <= 0) {
                        // 删除库存记录
                        inventoryRepository.deleteById(inventory.getId());
                    } else {
                        // 更新库存数量
                        inventory.setQty(newQty);
                        inventory.setLockedQty(Math.max(0, newLockedQty));
                        inventory.setUpdateTime(LocalDateTime.now());
                        inventoryRepository.updateById(inventory);
                    }

                    // 记录库存事务
                    InventoryTransaction transaction = new InventoryTransaction();
                    transaction.setTransactionNo(generateTransactionNo());
                    transaction.setTransactionType(InventoryTransaction.TYPE_OUTBOUND);
                    transaction.setWarehouseId(inventory.getWarehouseId());
                    transaction.setLocationId(item.getLocationId());
                    transaction.setLocationCode(item.getLocationCode());
                    transaction.setProductId(item.getProductId());
                    transaction.setSkuCode(item.getSkuCode());
                    transaction.setQtyChange(-item.getExceptionQty());
                    transaction.setQtyBefore(inventory.getQty() + item.getExceptionQty());
                    transaction.setQtyAfter(newQty > 0 ? newQty : 0);
                    transaction.setRefOrderType("EXCEPTION_UNDO_ISOLATE");
                    transaction.setRefOrderId(order.getId());
                    transaction.setRefOrderNo(order.getOrderNo());
                    transaction.setRemark("撤销隔离入库");
                    transaction.setCreateUser(userId);
                    transaction.setCreateTime(LocalDateTime.now());
                    transactionRepository.insert(transaction);
                }

                // 清除明细的库位信息
                item.setLocationId(null);
                item.setLocationCode(null);
                item.setStatus(ExceptionItem.STATUS_PENDING);
                item.setUpdateTime(LocalDateTime.now());
                itemRepository.updateById(item);
            }
        }

        // 主单状态回滚为"待处理"
        order.setStatus(ExceptionOrder.STATUS_PENDING);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        return getExceptionOrderById(orderId);
    }

    /**
     * 异常处理（退货/换货/报废/降价销售）
     */
    @Transactional
    public Map<String, Object> handle(Long orderId, HandleDTO dto, Long userId, String userName) {
        ExceptionOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("异常处理单不存在");
        }

        if (order.getStatus() != ExceptionOrder.STATUS_PROCESSING) {
            throw new RuntimeException("只有处理中状态的异常单可以执行处理操作");
        }

        // 查询明细
        LambdaQueryWrapper<ExceptionItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExceptionItem::getOrderId, orderId);
        List<ExceptionItem> items = itemRepository.selectList(wrapper);

        // 处理每个明细
        for (ExceptionItem item : items) {
            if (item.getStatus() != ExceptionItem.STATUS_ISOLATED) {
                continue;
            }

            item.setHandleType(dto.getHandleType());
            item.setHandleQty(item.getExceptionQty());
            item.setHandleResult(dto.getHandleResult());
            item.setStatus(ExceptionItem.STATUS_HANDLED);
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.updateById(item);

            // 扣减隔离库存
            LambdaQueryWrapper<Inventory> invWrapper = new LambdaQueryWrapper<>();
            invWrapper.eq(Inventory::getLocationId, item.getLocationId())
                    .eq(Inventory::getProductId, item.getProductId())
                    .eq(Inventory::getLockedQty, item.getExceptionQty());
            Inventory inventory = inventoryRepository.selectOne(invWrapper);
            if (inventory != null) {
                int newQty = inventory.getQty() - item.getExceptionQty();
                if (newQty <= 0) {
                    inventoryRepository.deleteById(inventory.getId());
                } else {
                    inventory.setQty(newQty);
                    inventory.setLockedQty(newQty);
                    inventory.setUpdateTime(LocalDateTime.now());
                    inventoryRepository.updateById(inventory);
                }

                // 记录库存事务
                int transType = getTransactionType(dto.getHandleType());
                InventoryTransaction transaction = new InventoryTransaction();
                transaction.setTransactionNo(generateTransactionNo());
                transaction.setTransactionType(transType);
                transaction.setWarehouseId(order.getWarehouseId());
                transaction.setLocationId(item.getLocationId());
                transaction.setLocationCode(item.getLocationCode());
                transaction.setProductId(item.getProductId());
                transaction.setSkuCode(item.getSkuCode());
                transaction.setBatchNo(item.getBatchNo());
                transaction.setQtyChange(-item.getExceptionQty());
                transaction.setQtyBefore(inventory.getQty() + item.getExceptionQty());
                transaction.setQtyAfter(Math.max(newQty, 0));
                transaction.setRefOrderType("EXCEPTION");
                transaction.setRefOrderId(order.getId());
                transaction.setRefOrderNo(order.getOrderNo());
                transaction.setRemark(getHandleTypeName(dto.getHandleType()) + ": " + dto.getHandleResult());
                transaction.setCreateUser(userId);
                transaction.setCreateTime(LocalDateTime.now());
                transactionRepository.insert(transaction);
            }

            // 降价销售时，将库存状态改为正常
            if (dto.getHandleType() == ExceptionOrder.HANDLE_DISCOUNT) {
                Inventory normalInventory = new Inventory();
                normalInventory.setWarehouseId(order.getWarehouseId());
                normalInventory.setWarehouseCode(order.getWarehouseCode());
                normalInventory.setLocationId(item.getLocationId());
                normalInventory.setLocationCode(item.getLocationCode());
                normalInventory.setProductId(item.getProductId());
                normalInventory.setSkuCode(item.getSkuCode());
                normalInventory.setProductName(item.getProductName());
                normalInventory.setBatchNo(item.getBatchNo());
                normalInventory.setQty(item.getExceptionQty());
                normalInventory.setAvailableQty(item.getExceptionQty());
                normalInventory.setLockedQty(0);
                normalInventory.setInboundOrderId(order.getInboundOrderId());
                normalInventory.setInboundOrderNo(order.getOrderNo());
                normalInventory.setInboundTime(LocalDateTime.now());
                normalInventory.setCreateTime(LocalDateTime.now());
                normalInventory.setUpdateTime(LocalDateTime.now());
                inventoryRepository.insert(normalInventory);
            }
        }

        // 更新主单
        order.setHandleType(dto.getHandleType());
        order.setHandleResult(dto.getHandleResult());
        order.setHandleTime(LocalDateTime.now());
        order.setHandleUserId(userId);
        order.setHandleUserName(userName);
        order.setStatus(ExceptionOrder.STATUS_COMPLETED);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        return getExceptionOrderById(orderId);
    }

    /**
     * 取消异常处理单
     *
     * 业务逻辑：
     * 1. 数量恢复：异常商品数量恢复到原入库单的待收货数量
     * 2. 库存处理：如果已执行隔离入库，隔离库存自动扣减，释放库位
     * 3. 操作记录：记录取消原因、取消人、取消时间
     */
    @Transactional
    public Map<String, Object> cancel(Long orderId, String cancelReason, Long userId, String userName) {
        ExceptionOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("异常处理单不存在");
        }

        if (order.getStatus() == ExceptionOrder.STATUS_COMPLETED) {
            throw new RuntimeException("已完成的异常单不可取消");
        }

        if (order.getStatus() == ExceptionOrder.STATUS_PROCESSING) {
            throw new RuntimeException("处理中的异常单需先撤销隔离入库，再取消");
        }

        // 查询异常明细
        LambdaQueryWrapper<ExceptionItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(ExceptionItem::getOrderId, orderId);
        List<ExceptionItem> items = itemRepository.selectList(itemWrapper);

        // 注意：验收/收货记录的处理在后面统一处理（步骤4.5）
        // 这里不再单独处理，避免重复且错误的逻辑

        // 2. 库存处理：如果已隔离入库，扣减隔离库存
        for (ExceptionItem item : items) {
            if (item.getStatus() == ExceptionItem.STATUS_ISOLATED && item.getLocationId() != null) {
                // 查找隔离库存（通过库位和商品，且 lockedQty > 0）
                LambdaQueryWrapper<Inventory> invWrapper = new LambdaQueryWrapper<>();
                invWrapper.eq(Inventory::getLocationId, item.getLocationId())
                         .eq(Inventory::getProductId, item.getProductId())
                         .gt(Inventory::getLockedQty, 0); // 有锁定数量表示隔离库存
                Inventory inventory = inventoryRepository.selectOne(invWrapper);

                if (inventory != null) {
                    int newQty = inventory.getQty() - item.getExceptionQty();
                    int newLockedQty = inventory.getLockedQty() - item.getExceptionQty();

                    if (newQty <= 0) {
                        // 删除库存记录
                        inventoryRepository.deleteById(inventory.getId());
                    } else {
                        // 更新库存数量
                        inventory.setQty(newQty);
                        inventory.setLockedQty(Math.max(0, newLockedQty));
                        inventory.setUpdateTime(LocalDateTime.now());
                        inventoryRepository.updateById(inventory);
                    }

                    // 记录库存事务
                    InventoryTransaction transaction = new InventoryTransaction();
                    transaction.setTransactionNo(generateTransactionNo());
                    transaction.setTransactionType(InventoryTransaction.TYPE_OUTBOUND);
                    transaction.setWarehouseId(inventory.getWarehouseId());
                    transaction.setLocationId(item.getLocationId());
                    transaction.setLocationCode(item.getLocationCode());
                    transaction.setProductId(item.getProductId());
                    transaction.setSkuCode(item.getSkuCode());
                    transaction.setQtyChange(-item.getExceptionQty());
                    transaction.setQtyBefore(inventory.getQty() + item.getExceptionQty());
                    transaction.setQtyAfter(newQty > 0 ? newQty : 0);
                    transaction.setRefOrderType("EXCEPTION_CANCEL");
                    transaction.setRefOrderId(order.getId());
                    transaction.setRefOrderNo(order.getOrderNo());
                    transaction.setRemark("异常取消，释放隔离库存");
                    transaction.setCreateUser(userId);
                    transaction.setCreateTime(LocalDateTime.now());
                    transactionRepository.insert(transaction);
                }
            }
        }

        // 3. 更新主单状态
        order.setStatus(ExceptionOrder.STATUS_CANCELLED);
        String remark = order.getRemark() != null ? order.getRemark() : "";
        order.setRemark(remark + " | 取消原因: " + cancelReason + " | 取消人: " + userName);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        // 4. 更新明细状态
        for (ExceptionItem item : items) {
            item.setStatus(ExceptionItem.STATUS_HANDLED);
            item.setHandleResult("取消: " + cancelReason);
            item.setUpdateTime(LocalDateTime.now());
            itemRepository.updateById(item);
        }

        // 4.5 处理相关的验收/收货记录
        // 注意：不能把整条记录标记为已取消，因为验收记录同时包含合格数量和不合格数量
        // 正确做法：只取消异常相关的数量部分，保留正常数据
        if (order.getSourceType() == ExceptionOrder.SOURCE_INSPECT) {
            // 验收异常取消：不合格数量被取消，合格数量保留
            // 将验收记录的 rejectedQty 设为 0，保留 qualifiedQty
            for (ExceptionItem exceptionItem : items) {
                if (exceptionItem.getInboundItemId() != null) {
                    LambdaQueryWrapper<InspectRecord> inspectWrapper = new LambdaQueryWrapper<>();
                    inspectWrapper.eq(InspectRecord::getInboundItemId, exceptionItem.getInboundItemId())
                                   .gt(InspectRecord::getRejectedQty, 0);
                    List<InspectRecord> records = inspectRecordRepository.selectList(inspectWrapper);
                    for (InspectRecord record : records) {
                        // 不标记整条记录为已取消，只清空不合格数量
                        // 合格数量保留，这样上架数据不受影响
                        record.setRejectedQty(0);
                        record.setRemark((record.getRemark() != null ? record.getRemark() : "") + " | 异常取消，不合格数量清零");
                        inspectRecordRepository.updateById(record);
                    }
                    log.info("验收异常取消，清零不合格数量，保留合格数量: 入库明细ID={}", exceptionItem.getInboundItemId());
                }
            }
        } else if (order.getSourceType() == ExceptionOrder.SOURCE_RECEIVE) {
            // 收货异常取消：差异数量被取消，正常收货数量保留
            // 收货记录的 diffQty 表示差异（少货），receiveQty 表示实际收货
            // 取消异常时，差异数量应该变为0（这部分商品可以重新收货）
            for (ExceptionItem exceptionItem : items) {
                if (exceptionItem.getInboundItemId() != null) {
                    LambdaQueryWrapper<ReceiveRecord> receiveWrapper = new LambdaQueryWrapper<>();
                    receiveWrapper.eq(ReceiveRecord::getInboundItemId, exceptionItem.getInboundItemId())
                                   .lt(ReceiveRecord::getDiffQty, 0); // 差异数量小于0表示少货
                    List<ReceiveRecord> records = receiveRecordRepository.selectList(receiveWrapper);
                    for (ReceiveRecord record : records) {
                        // 清零差异数量，保留实际收货数量
                        record.setDiffQty(0);
                        record.setDiffReason(null);
                        record.setRemark((record.getRemark() != null ? record.getRemark() : "") + " | 异常取消，差异数量清零");
                        receiveRecordRepository.updateById(record);
                    }
                    log.info("收货异常取消，清零差异数量，保留收货数量: 入库明细ID={}", exceptionItem.getInboundItemId());
                }
            }
        }

        // 5. 使用统一的状态计算服务更新入库单状态和进度
        // 这确保了无论当前状态是什么，都会根据实际进度正确计算状态
        if (order.getInboundOrderId() != null) {
            log.info("取消异常后重新计算入库单状态: {}", inboundStatusService.getProgressSummary(order.getInboundOrderId()));
            inboundStatusService.recalculateStatus(order.getInboundOrderId());
        }

        return getExceptionOrderById(orderId);
    }

    /**
     * 创建补货入库单
     *
     * 业务场景：供应商对异常商品进行补货（退货/换货后）
     * 流程：
     * 1. 从异常处理单获取商品明细
     * 2. 创建补货入库单（类型=6）
     * 3. 关联异常处理单
     */
    @Transactional
    public Map<String, Object> createReplacementInbound(Long orderId, Long userId, String userName) {
        ExceptionOrder order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("异常处理单不存在");
        }

        // 只有已完成的异常单才能创建补货入库单
        if (order.getStatus() != ExceptionOrder.STATUS_COMPLETED) {
            throw new RuntimeException("只有已完成的异常单才能创建补货入库单");
        }

        // 退货、换货、报废都可以创建补货入库单（降价销售不需要补货）
        if (order.getHandleType() != null && order.getHandleType() == ExceptionOrder.HANDLE_DISCOUNT) {
            throw new RuntimeException("降价销售的异常单不需要创建补货入库单");
        }

        // 检查是否已创建补货入库单
        if (order.getReplacementInboundOrderId() != null) {
            throw new RuntimeException("该异常单已创建补货入库单: " + order.getReplacementInboundOrderNo());
        }

        // 查询异常明细
        LambdaQueryWrapper<ExceptionItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(ExceptionItem::getOrderId, orderId);
        List<ExceptionItem> items = itemRepository.selectList(itemWrapper);

        if (items.isEmpty()) {
            throw new RuntimeException("异常明细为空，无法创建补货入库单");
        }

        // 生成补货入库单号
        String inboundOrderNo = generateInboundOrderNo();
        // 生成补货送货批次号 RP-日期-序号
        String deliveryBatchNo = generateReplacementBatchNo();

        // 获取原入库单的采购订单号
        String originalPoNo = null;
        if (order.getInboundOrderId() != null) {
            InboundOrder originalInbound = inboundOrderRepository.selectById(order.getInboundOrderId());
            if (originalInbound != null) {
                originalPoNo = originalInbound.getPoNo();
            }
        }

        // 创建补货入库单
        InboundOrder inboundOrder = new InboundOrder();
        inboundOrder.setOrderNo(inboundOrderNo);
        inboundOrder.setDeliveryBatchNo(deliveryBatchNo);
        inboundOrder.setOrderType(InboundOrder.TYPE_REPLACEMENT);
        inboundOrder.setPoNo(originalPoNo);  // 继承原采购订单号
        inboundOrder.setSupplierId(order.getSupplierId());
        inboundOrder.setSupplierCode(order.getSupplierCode());
        inboundOrder.setSupplierName(order.getSupplierName());
        inboundOrder.setWarehouseId(order.getWarehouseId());
        inboundOrder.setWarehouseCode(order.getWarehouseCode());
        // 需要查询仓库名称
        inboundOrder.setWarehouseName(getWarehouseName(order.getWarehouseId()));
        inboundOrder.setRefExceptionOrderId(orderId);
        inboundOrder.setRefExceptionOrderNo(order.getOrderNo());
        inboundOrder.setStatus(InboundOrder.STATUS_PENDING);
        inboundOrder.setRemark("异常处理单 " + order.getOrderNo() + " 补货入库");
        inboundOrder.setCreateUser(userId);
        inboundOrder.setCreateTime(LocalDateTime.now());
        inboundOrder.setUpdateTime(LocalDateTime.now());

        // 计算总预期数量
        int totalExpectedQty = items.stream()
                .mapToInt(ExceptionItem::getExceptionQty)
                .sum();
        inboundOrder.setTotalExpectedQty(totalExpectedQty);
        inboundOrder.setTotalReceivedQty(0);
        inboundOrder.setTotalQualifiedQty(0);
        inboundOrder.setTotalRejectedQty(0);
        inboundOrder.setTotalPutawayQty(0);
        inboundOrder.setTotalReturnQty(0);
        inboundOrder.setProgressReceive(0);
        inboundOrder.setProgressInspect(0);
        inboundOrder.setProgressPutaway(0);

        inboundOrderRepository.insert(inboundOrder);

        // 创建入库单明细
        for (ExceptionItem item : items) {
            InboundOrderItem inboundItem = new InboundOrderItem();
            inboundItem.setOrderId(inboundOrder.getId());
            inboundItem.setOrderNo(inboundOrderNo);
            inboundItem.setProductId(item.getProductId());
            inboundItem.setSkuCode(item.getSkuCode());
            inboundItem.setProductName(item.getProductName());
            inboundItem.setBarcode(item.getBarcode());
            inboundItem.setExpectedQty(item.getExceptionQty());
            inboundItem.setReceivedQty(0);
            inboundItem.setQualifiedQty(0);
            inboundItem.setRejectedQty(0);
            inboundItem.setPutawayQty(0);
            inboundItem.setReturnQty(0);
            inboundItem.setStatus(InboundOrderItem.STATUS_PENDING);
            inboundOrderItemRepository.insert(inboundItem);
        }

        // 更新异常处理单的补货入库单关联
        order.setReplacementInboundOrderId(inboundOrder.getId());
        order.setReplacementInboundOrderNo(inboundOrderNo);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.updateById(order);

        return getExceptionOrderById(orderId);
    }

    /**
     * 生成入库单号 IN+年月日+4位序号
     */
    private String generateInboundOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "IN" + dateStr;

        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(InboundOrder::getOrderNo, prefix)
                .orderByDesc(InboundOrder::getOrderNo)
                .last("LIMIT 1");

        InboundOrder lastOrder = inboundOrderRepository.selectOne(wrapper);

        int seq = 1;
        if (lastOrder != null && lastOrder.getOrderNo() != null) {
            String lastNo = lastOrder.getOrderNo();
            if (lastNo.startsWith(prefix)) {
                seq = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
            }
        }

        return prefix + String.format("%04d", seq);
    }

    /**
     * 生成补货送货批次号 RP-日期-序号
     */
    private String generateReplacementBatchNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "RP-" + dateStr + "-" + String.format("%03d", (int)(Math.random() * 1000));
    }

    /**
     * 获取仓库名称
     */
    private String getWarehouseName(Long warehouseId) {
        if (warehouseId == null) return null;
        // 简单实现：从入库单查询
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InboundOrder::getWarehouseId, warehouseId)
                .isNotNull(InboundOrder::getWarehouseName)
                .last("LIMIT 1");
        InboundOrder order = inboundOrderRepository.selectOne(wrapper);
        return order != null ? order.getWarehouseName() : null;
    }

    /**
     * 查询异常统计
     */
    public Map<String, Object> getStatistics(String startTime, String endTime) {
        LambdaQueryWrapper<ExceptionOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(ExceptionOrder::getCreateTime, LocalDateTime.parse(startTime + "T00:00:00"));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(ExceptionOrder::getCreateTime, LocalDateTime.parse(endTime + "T23:59:59"));
        }

        List<ExceptionOrder> orders = orderRepository.selectList(wrapper);

        // 按异常类型统计
        Map<Integer, Long> typeCount = orders.stream()
                .collect(Collectors.groupingBy(ExceptionOrder::getExceptionType, Collectors.counting()));

        // 按供应商统计
        Map<String, Long> supplierCount = orders.stream()
                .filter(o -> o.getSupplierName() != null)
                .collect(Collectors.groupingBy(ExceptionOrder::getSupplierName, Collectors.counting()));

        // 按状态统计
        Map<Integer, Long> statusCount = orders.stream()
                .collect(Collectors.groupingBy(ExceptionOrder::getStatus, Collectors.counting()));

        // 总数量
        int totalQty = orders.stream()
                .mapToInt(ExceptionOrder::getTotalQty)
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", orders.size());
        result.put("totalQty", totalQty);
        result.put("byType", typeCount);
        result.put("bySupplier", supplierCount);
        result.put("byStatus", statusCount);
        return result;
    }

    private String generateTransactionNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "TX" + dateStr + String.format("%04d", (int)(Math.random() * 10000));
    }

    private int getTransactionType(Integer handleType) {
        switch (handleType) {
            case 1: return InventoryTransaction.TYPE_RETURN;
            case 2: return InventoryTransaction.TYPE_OUTBOUND;
            case 3: return InventoryTransaction.TYPE_OUTBOUND;
            case 4: return InventoryTransaction.TYPE_TRANSFER_IN;
            default: return InventoryTransaction.TYPE_OUTBOUND;
        }
    }

    private String getHandleTypeName(Integer handleType) {
        switch (handleType) {
            case 1: return "退货";
            case 2: return "换货";
            case 3: return "报废";
            case 4: return "降价销售";
            default: return "其他";
        }
    }

    private Map<String, Object> toOrderMap(ExceptionOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("inboundOrderId", order.getInboundOrderId());
        map.put("inboundOrderNo", order.getInboundOrderNo());
        map.put("purchaseOrderId", order.getPurchaseOrderId());
        map.put("purchaseOrderNo", order.getPurchaseOrderNo());
        map.put("supplierId", order.getSupplierId());
        map.put("supplierCode", order.getSupplierCode());
        map.put("supplierName", order.getSupplierName());
        map.put("warehouseId", order.getWarehouseId());
        map.put("warehouseCode", order.getWarehouseCode());
        map.put("zoneId", order.getZoneId());
        map.put("zoneCode", order.getZoneCode());
        map.put("exceptionType", order.getExceptionType());
        map.put("totalQty", order.getTotalQty());
        map.put("exceptionReason", order.getExceptionReason());
        map.put("status", order.getStatus());
        map.put("handleType", order.getHandleType());
        map.put("handleResult", order.getHandleResult());
        map.put("handleTime", order.getHandleTime());
        map.put("handleUserId", order.getHandleUserId());
        map.put("handleUserName", order.getHandleUserName());
        map.put("sourceType", order.getSourceType());
        map.put("replacementInboundOrderId", order.getReplacementInboundOrderId());
        map.put("replacementInboundOrderNo", order.getReplacementInboundOrderNo());
        map.put("remark", order.getRemark());
        map.put("createUserId", order.getCreateUserId());
        map.put("createUserName", order.getCreateUserName());
        map.put("createTime", order.getCreateTime());
        return map;
    }

    private Map<String, Object> toItemMap(ExceptionItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("orderId", item.getOrderId());
        map.put("orderNo", item.getOrderNo());
        map.put("productId", item.getProductId());
        map.put("skuCode", item.getSkuCode());
        map.put("productName", item.getProductName());
        map.put("barcode", item.getBarcode());
        map.put("batchNo", item.getBatchNo());
        map.put("exceptionQty", item.getExceptionQty());
        map.put("exceptionType", item.getExceptionType());
        map.put("exceptionReason", item.getExceptionReason());
        map.put("locationId", item.getLocationId());
        map.put("locationCode", item.getLocationCode());
        map.put("status", item.getStatus());
        map.put("handleType", item.getHandleType());
        map.put("handleQty", item.getHandleQty());
        map.put("handleResult", item.getHandleResult());
        map.put("inboundItemId", item.getInboundItemId());
        map.put("createTime", item.getCreateTime());
        return map;
    }
}