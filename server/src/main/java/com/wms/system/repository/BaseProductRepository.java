package com.wms.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.system.entity.BaseProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BaseProductRepository extends BaseMapper<BaseProduct> {

    /**
     * 按条码查询商品
     */
    @Select("SELECT * FROM base_product WHERE barcode = #{barcode} LIMIT 1")
    BaseProduct findByBarcode(@Param("barcode") String barcode);

    /**
     * 按SKU编码查询商品
     */
    @Select("SELECT * FROM base_product WHERE sku_code = #{skuCode} LIMIT 1")
    BaseProduct findBySkuCode(@Param("skuCode") String skuCode);
}
