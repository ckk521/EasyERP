package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.SupplierProductDTO;
import com.wms.system.entity.BaseProduct;
import com.wms.system.entity.BaseSupplier;
import com.wms.system.entity.BaseSupplierProduct;
import com.wms.system.repository.BaseProductRepository;
import com.wms.system.repository.BaseSupplierProductRepository;
import com.wms.system.repository.BaseSupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierProductService {

    private final BaseSupplierProductRepository supplierProductRepository;
    private final BaseSupplierRepository supplierRepository;
    private final BaseProductRepository productRepository;

    public Map<String, Object> list(Long supplierId, Long productId, PageDTO pageDTO) {
        LambdaQueryWrapper<BaseSupplierProduct> wrapper = new LambdaQueryWrapper<>();

        if (supplierId != null) {
            wrapper.eq(BaseSupplierProduct::getSupplierId, supplierId);
        }
        if (productId != null) {
            wrapper.eq(BaseSupplierProduct::getProductId, productId);
        }
        wrapper.orderByDesc(BaseSupplierProduct::getCreateTime);

        IPage<BaseSupplierProduct> page = supplierProductRepository.selectPage(
            new Page<>(pageDTO.getPage(), pageDTO.getLimit()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    public Map<String, Object> getById(Long id) {
        BaseSupplierProduct sp = supplierProductRepository.selectById(id);
        if (sp == null) {
            throw new RuntimeException("供应商商品关系不存在");
        }
        return toMap(sp);
    }

    public Long create(SupplierProductDTO dto) {
        BaseSupplier supplier = supplierRepository.selectById(dto.getSupplierId());
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }

        BaseProduct product = productRepository.selectById(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        LambdaQueryWrapper<BaseSupplierProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseSupplierProduct::getSupplierId, dto.getSupplierId())
               .eq(BaseSupplierProduct::getProductId, dto.getProductId());
        if (supplierProductRepository.selectCount(wrapper) > 0) {
            throw new RuntimeException("该供应商已关联此商品");
        }

        BaseSupplierProduct sp = new BaseSupplierProduct();
        sp.setSupplierId(dto.getSupplierId());
        sp.setSupplierCode(supplier.getCode());
        sp.setSupplierName(supplier.getName());
        sp.setProductId(dto.getProductId());
        sp.setSkuCode(product.getSkuCode());
        sp.setProductName(product.getNameCn());
        sp.setSupplierSkuCode(dto.getSupplierSkuCode());
        sp.setPurchasePrice(dto.getPurchasePrice());
        sp.setMinOrderQty(dto.getMinOrderQty());
        sp.setLeadTime(dto.getLeadTime());
        sp.setStatus(1);

        supplierProductRepository.insert(sp);
        return sp.getId();
    }

    public void update(Long id, SupplierProductDTO dto) {
        BaseSupplierProduct sp = supplierProductRepository.selectById(id);
        if (sp == null) {
            throw new RuntimeException("供应商商品关系不存在");
        }

        sp.setSupplierSkuCode(dto.getSupplierSkuCode());
        sp.setPurchasePrice(dto.getPurchasePrice());
        sp.setMinOrderQty(dto.getMinOrderQty());
        sp.setLeadTime(dto.getLeadTime());

        supplierProductRepository.updateById(sp);
    }

    public void delete(Long id) {
        supplierProductRepository.deleteById(id);
    }

    public void enable(Long id) {
        BaseSupplierProduct sp = supplierProductRepository.selectById(id);
        if (sp == null) {
            throw new RuntimeException("供应商商品关系不存在");
        }
        sp.setStatus(1);
        supplierProductRepository.updateById(sp);
    }

    public void disable(Long id) {
        BaseSupplierProduct sp = supplierProductRepository.selectById(id);
        if (sp == null) {
            throw new RuntimeException("供应商商品关系不存在");
        }
        sp.setStatus(0);
        supplierProductRepository.updateById(sp);
    }

    public List<Map<String, Object>> getSuppliersByProduct(Long productId) {
        LambdaQueryWrapper<BaseSupplierProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseSupplierProduct::getProductId, productId)
               .eq(BaseSupplierProduct::getStatus, 1);

        return supplierProductRepository.selectList(wrapper).stream()
            .map(this::toMap)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getProductsBySupplier(Long supplierId) {
        LambdaQueryWrapper<BaseSupplierProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseSupplierProduct::getSupplierId, supplierId)
               .eq(BaseSupplierProduct::getStatus, 1);

        return supplierProductRepository.selectList(wrapper).stream()
            .map(this::toMap)
            .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(BaseSupplierProduct sp) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sp.getId());
        map.put("supplierId", sp.getSupplierId());
        map.put("supplierCode", sp.getSupplierCode());
        map.put("supplierName", sp.getSupplierName());
        map.put("productId", sp.getProductId());
        map.put("skuCode", sp.getSkuCode());
        map.put("productName", sp.getProductName());
        map.put("supplierSkuCode", sp.getSupplierSkuCode());
        map.put("purchasePrice", sp.getPurchasePrice());
        map.put("minOrderQty", sp.getMinOrderQty());
        map.put("leadTime", sp.getLeadTime());
        map.put("status", sp.getStatus());
        map.put("createTime", sp.getCreateTime());
        return map;
    }
}
