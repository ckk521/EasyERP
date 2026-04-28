package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.system.dto.PageDTO;
import com.wms.system.dto.ProductDTO;
import com.wms.system.entity.BaseCategory;
import com.wms.system.entity.BaseProduct;
import com.wms.system.repository.BaseCategoryRepository;
import com.wms.system.repository.BaseProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final BaseProductRepository productRepository;
    private final BaseCategoryRepository categoryRepository;

    public Map<String, Object> listProducts(PageDTO pageDTO, String keyword, Long categoryId, Integer status) {
        LambdaQueryWrapper<BaseProduct> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(BaseProduct::getNameCn, keyword)
                   .or().like(BaseProduct::getSkuCode, keyword)
                   .or().like(BaseProduct::getBarcode, keyword);
        }
        if (categoryId != null) {
            wrapper.eq(BaseProduct::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(BaseProduct::getStatus, status);
        }
        wrapper.orderByDesc(BaseProduct::getCreateTime);

        IPage<BaseProduct> page = productRepository.selectPage(
            new Page<>(pageDTO.getPage(), pageDTO.getLimit()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    public Map<String, Object> getProductById(Long id) {
        BaseProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        return toMap(product);
    }

    public Long createProduct(ProductDTO dto) {
        LambdaQueryWrapper<BaseProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseProduct::getSkuCode, dto.getSkuCode());
        if (productRepository.selectCount(wrapper) > 0) {
            throw new RuntimeException("SKU编码已存在");
        }

        BaseProduct product = new BaseProduct();
        product.setSkuCode(dto.getSkuCode());
        product.setBarcode(dto.getBarcode());
        product.setNameCn(dto.getNameCn());
        product.setNameEn(dto.getNameEn());
        product.setCategoryId(dto.getCategoryId());
        product.setBrand(dto.getBrand());
        product.setWeight(dto.getWeight());
        product.setLength(dto.getLength());
        product.setWidth(dto.getWidth());
        product.setHeight(dto.getHeight());
        product.setMainImage(dto.getMainImage());
        product.setDescription(dto.getDescription());
        product.setStorageCond(dto.getStorageCond() != null ? dto.getStorageCond() : 1);
        product.setShelfLife(dto.getShelfLife());
        product.setExpiryWarning(dto.getExpiryWarning() != null ? dto.getExpiryWarning() : 30);
        product.setIsDangerous(dto.getIsDangerous() != null ? dto.getIsDangerous() : 0);
        product.setIsFragile(dto.getIsFragile() != null ? dto.getIsFragile() : 0);
        product.setIsHighValue(dto.getIsHighValue() != null ? dto.getIsHighValue() : 0);
        product.setNeedExpiryMgmt(dto.getNeedExpiryMgmt() != null ? dto.getNeedExpiryMgmt() : 0);
        product.setStatus(1);

        // 计算体积
        if (dto.getLength() != null && dto.getWidth() != null && dto.getHeight() != null) {
            product.setVolume(dto.getLength().multiply(dto.getWidth()).multiply(dto.getHeight()));
        }

        // 获取分类名称
        if (dto.getCategoryId() != null) {
            BaseCategory category = categoryRepository.selectById(dto.getCategoryId());
            if (category != null) {
                product.setCategoryName(category.getName());
            }
        }

        productRepository.insert(product);
        return product.getId();
    }

    public void updateProduct(Long id, ProductDTO dto) {
        BaseProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        product.setNameCn(dto.getNameCn());
        product.setNameEn(dto.getNameEn());
        product.setCategoryId(dto.getCategoryId());
        product.setBrand(dto.getBrand());
        product.setWeight(dto.getWeight());
        product.setLength(dto.getLength());
        product.setWidth(dto.getWidth());
        product.setHeight(dto.getHeight());
        product.setMainImage(dto.getMainImage());
        product.setDescription(dto.getDescription());
        product.setStorageCond(dto.getStorageCond());
        product.setShelfLife(dto.getShelfLife());
        product.setExpiryWarning(dto.getExpiryWarning());
        product.setIsDangerous(dto.getIsDangerous());
        product.setIsFragile(dto.getIsFragile());
        product.setIsHighValue(dto.getIsHighValue());
        product.setNeedExpiryMgmt(dto.getNeedExpiryMgmt());

        // 计算体积
        if (dto.getLength() != null && dto.getWidth() != null && dto.getHeight() != null) {
            product.setVolume(dto.getLength().multiply(dto.getWidth()).multiply(dto.getHeight()));
        }

        // 获取分类名称
        if (dto.getCategoryId() != null) {
            BaseCategory category = categoryRepository.selectById(dto.getCategoryId());
            if (category != null) {
                product.setCategoryName(category.getName());
            }
        }

        productRepository.updateById(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public void enableProduct(Long id) {
        BaseProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setStatus(1);
        productRepository.updateById(product);
    }

    public void disableProduct(Long id) {
        BaseProduct product = productRepository.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setStatus(0);
        productRepository.updateById(product);
    }

    private Map<String, Object> toMap(BaseProduct product) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", product.getId());
        map.put("skuCode", product.getSkuCode());
        map.put("barcode", product.getBarcode());
        map.put("nameCn", product.getNameCn());
        map.put("nameEn", product.getNameEn());
        map.put("categoryId", product.getCategoryId());
        map.put("categoryName", product.getCategoryName());
        map.put("brand", product.getBrand());
        map.put("weight", product.getWeight());
        map.put("length", product.getLength());
        map.put("width", product.getWidth());
        map.put("height", product.getHeight());
        map.put("volume", product.getVolume());
        map.put("mainImage", product.getMainImage());
        map.put("storageCond", product.getStorageCond());
        map.put("shelfLife", product.getShelfLife());
        map.put("expiryWarning", product.getExpiryWarning());
        map.put("isDangerous", product.getIsDangerous());
        map.put("isFragile", product.getIsFragile());
        map.put("isHighValue", product.getIsHighValue());
        map.put("needExpiryMgmt", product.getNeedExpiryMgmt());
        map.put("status", product.getStatus());
        map.put("createTime", product.getCreateTime());
        return map;
    }
}
