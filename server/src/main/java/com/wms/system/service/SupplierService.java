package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.SupplierDTO;
import com.wms.system.entity.BaseSupplier;
import com.wms.system.repository.BaseSupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final BaseSupplierRepository supplierRepository;

    public Map<String, Object> listSuppliers(PageDTO pageDTO, String keyword, Integer status) {
        LambdaQueryWrapper<BaseSupplier> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(BaseSupplier::getName, keyword)
                   .or().like(BaseSupplier::getCode, keyword);
        }
        if (status != null) {
            wrapper.eq(BaseSupplier::getStatus, status);
        }
        wrapper.orderByDesc(BaseSupplier::getCreateTime);

        IPage<BaseSupplier> page = supplierRepository.selectPage(
            new Page<>(pageDTO.getPage(), pageDTO.getLimit()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    public Map<String, Object> getSupplierById(Long id) {
        BaseSupplier supplier = supplierRepository.selectById(id);
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }
        return toMap(supplier);
    }

    public Long createSupplier(SupplierDTO dto) {
        LambdaQueryWrapper<BaseSupplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseSupplier::getCode, dto.getCode());
        if (supplierRepository.selectCount(wrapper) > 0) {
            throw new RuntimeException("供应商编码已存在");
        }

        BaseSupplier supplier = new BaseSupplier();
        supplier.setCode(dto.getCode());
        supplier.setName(dto.getName());
        supplier.setContact(dto.getContact());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        supplier.setBankName(dto.getBankName());
        supplier.setBankAccount(dto.getBankAccount());
        supplier.setTaxNo(dto.getTaxNo());
        supplier.setRemark(dto.getRemark());
        supplier.setStatus(1);

        supplierRepository.insert(supplier);
        return supplier.getId();
    }

    public void updateSupplier(Long id, SupplierDTO dto) {
        BaseSupplier supplier = supplierRepository.selectById(id);
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }

        supplier.setName(dto.getName());
        supplier.setContact(dto.getContact());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        supplier.setBankName(dto.getBankName());
        supplier.setBankAccount(dto.getBankAccount());
        supplier.setTaxNo(dto.getTaxNo());
        supplier.setRemark(dto.getRemark());

        supplierRepository.updateById(supplier);
    }

    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }

    public void enableSupplier(Long id) {
        BaseSupplier supplier = supplierRepository.selectById(id);
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }
        supplier.setStatus(1);
        supplierRepository.updateById(supplier);
    }

    public void disableSupplier(Long id) {
        BaseSupplier supplier = supplierRepository.selectById(id);
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }
        supplier.setStatus(0);
        supplierRepository.updateById(supplier);
    }

    private Map<String, Object> toMap(BaseSupplier supplier) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", supplier.getId());
        map.put("code", supplier.getCode());
        map.put("name", supplier.getName());
        map.put("contact", supplier.getContact());
        map.put("phone", supplier.getPhone());
        map.put("email", supplier.getEmail());
        map.put("address", supplier.getAddress());
        map.put("bankName", supplier.getBankName());
        map.put("bankAccount", supplier.getBankAccount());
        map.put("taxNo", supplier.getTaxNo());
        map.put("remark", supplier.getRemark());
        map.put("status", supplier.getStatus());
        map.put("createTime", supplier.getCreateTime());
        return map;
    }
}
