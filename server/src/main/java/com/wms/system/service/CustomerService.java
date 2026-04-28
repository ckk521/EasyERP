package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.CustomerDTO;
import com.wms.system.dto.PageDTO;
import com.wms.system.entity.BaseCustomer;
import com.wms.system.repository.BaseCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final BaseCustomerRepository customerRepository;

    public Map<String, Object> listCustomers(PageDTO pageDTO, String keyword, Integer type, Integer status) {
        LambdaQueryWrapper<BaseCustomer> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(BaseCustomer::getName, keyword)
                   .or().like(BaseCustomer::getCode, keyword);
        }
        if (type != null) {
            wrapper.eq(BaseCustomer::getType, type);
        }
        if (status != null) {
            wrapper.eq(BaseCustomer::getStatus, status);
        }
        wrapper.orderByDesc(BaseCustomer::getCreateTime);

        IPage<BaseCustomer> page = customerRepository.selectPage(
            new Page<>(pageDTO.getPage(), pageDTO.getLimit()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    public Map<String, Object> getCustomerById(Long id) {
        BaseCustomer customer = customerRepository.selectById(id);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }
        return toMap(customer);
    }

    public Long createCustomer(CustomerDTO dto) {
        LambdaQueryWrapper<BaseCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseCustomer::getCode, dto.getCode());
        if (customerRepository.selectCount(wrapper) > 0) {
            throw new RuntimeException("客户编码已存在");
        }

        BaseCustomer customer = new BaseCustomer();
        customer.setCode(dto.getCode());
        customer.setName(dto.getName());
        customer.setType(dto.getType() != null ? dto.getType() : 1);
        customer.setContact(dto.getContact());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setCreditLimit(dto.getCreditLimit());
        customer.setLevel(dto.getLevel() != null ? dto.getLevel() : 1);
        customer.setRemark(dto.getRemark());
        customer.setStatus(1);

        customerRepository.insert(customer);
        return customer.getId();
    }

    public void updateCustomer(Long id, CustomerDTO dto) {
        BaseCustomer customer = customerRepository.selectById(id);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        customer.setName(dto.getName());
        customer.setType(dto.getType());
        customer.setContact(dto.getContact());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setCreditLimit(dto.getCreditLimit());
        customer.setLevel(dto.getLevel());
        customer.setRemark(dto.getRemark());

        customerRepository.updateById(customer);
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    public void enableCustomer(Long id) {
        BaseCustomer customer = customerRepository.selectById(id);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }
        customer.setStatus(1);
        customerRepository.updateById(customer);
    }

    public void disableCustomer(Long id) {
        BaseCustomer customer = customerRepository.selectById(id);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }
        customer.setStatus(0);
        customerRepository.updateById(customer);
    }

    private Map<String, Object> toMap(BaseCustomer customer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", customer.getId());
        map.put("code", customer.getCode());
        map.put("name", customer.getName());
        map.put("type", customer.getType());
        map.put("contact", customer.getContact());
        map.put("phone", customer.getPhone());
        map.put("email", customer.getEmail());
        map.put("address", customer.getAddress());
        map.put("creditLimit", customer.getCreditLimit());
        map.put("level", customer.getLevel());
        map.put("remark", customer.getRemark());
        map.put("status", customer.getStatus());
        map.put("createTime", customer.getCreateTime());
        return map;
    }
}
