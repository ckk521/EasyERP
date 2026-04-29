# ABC分类功能规划

> 创建时间: 2026-04-29
> 状态: 待开发（出库模块后实现）

---

## 一、功能概述

ABC分类是一种库存管理方法，根据商品的重要程度将商品分为三类，实现差异化管理。

| 分类 | 占比 | 特点 | 管理策略 |
|------|------|------|----------|
| **A类** | 约20%商品，贡献80%出库频次 | 高周转/高价值 | 重点管理，频繁盘点，黄金库位 |
| **B类** | 约30%商品，贡献15%出库频次 | 中等周转 | 常规管理 |
| **C类** | 约50%商品，贡献5%出库频次 | 低周转 | 简化管理，减少盘点 |

---

## 二、分类维度

建议以**出库频次**为主维度（WMS视角）：

| 维度 | 计算方式 | 适用场景 | 优先级 |
|------|----------|----------|--------|
| **出库频次ABC** | 按出库次数 | 仓储视角，关注高频商品 | **主维度** |
| 销售额ABC | 按销售金额 | 财务视角 | 可选 |
| 库存金额ABC | 按库存占用金额 | 资金视角 | 可选 |

---

## 三、数据模型

### 3.1 商品表扩展

```sql
ALTER TABLE base_product ADD COLUMN abc_category VARCHAR(1) DEFAULT 'B' COMMENT 'ABC分类: A/B/C';
ALTER TABLE base_product ADD COLUMN abc_category_type INT DEFAULT 1 COMMENT 'ABC分类维度: 1-出库频次 2-销售额 3-库存金额';
ALTER TABLE base_product ADD COLUMN abc_updated_at DATETIME COMMENT 'ABC分类更新时间';
ALTER TABLE base_product ADD COLUMN abc_updated_by BIGINT COMMENT 'ABC分类更新人(手动调整时)';
```

### 3.2 ABC分类配置表

```sql
CREATE TABLE wms_abc_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_type INT NOT NULL COMMENT '分类维度: 1-出库频次 2-销售额 3-库存金额',
    a_threshold DECIMAL(5,2) DEFAULT 80.00 COMMENT 'A类阈值(累计占比%)',
    b_threshold DECIMAL(5,2) DEFAULT 95.00 COMMENT 'B类阈值(累计占比%)',
    auto_update TINYINT DEFAULT 1 COMMENT '是否自动更新: 0-否 1-是',
    update_cycle INT DEFAULT 30 COMMENT '更新周期(天)',
    last_updated_at DATETIME COMMENT '上次更新时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 四、计算逻辑

### 4.1 自动计算流程

```
1. 统计各商品出库频次（近90天）
2. 按出库次数降序排列
3. 计算累计占比：
   - 累计占比 ≤ 80% → A类
   - 累计占比 ≤ 95% → B类
   - 累计占比 > 95% → C类
4. 更新商品ABC分类字段
```

### 4.2 计算服务代码

```java
/**
 * ABC分类计算服务
 */
@Service
public class AbcCategoryService {

    /**
     * 自动计算ABC分类（定时任务调用）
     */
    @Scheduled(cron = "0 0 2 1 * ?") // 每月1日凌晨2点执行
    public void autoCalculate() {
        // 1. 统计近90天出库频次
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);
        List<ProductOutboundStats> stats = outboundRepository.statsByProduct(startDate, endDate);

        // 2. 计算总出库次数
        int totalOutbound = stats.stream().mapToInt(ProductOutboundStats::getCount).sum();

        // 3. 按出库次数降序，计算累计占比
        double cumulative = 0;
        for (ProductOutboundStats stat : stats) {
            cumulative += (double) stat.getCount() / totalOutbound * 100;

            String category;
            if (cumulative <= 80) {
                category = "A";
            } else if (cumulative <= 95) {
                category = "B";
            } else {
                category = "C";
            }

            productRepository.updateAbcCategory(stat.getProductId(), category);
        }
    }

    /**
     * 手动调整ABC分类
     */
    public void manualAdjust(Long productId, String category, Long userId) {
        productRepository.updateAbcCategory(productId, category, userId, LocalDateTime.now());
    }
}
```

---

## 五、与盘点功能联动

### 5.1 差异化盘点策略

| ABC分类 | 盘点方式 | 盘点频率 | 复盘要求 |
|---------|----------|----------|----------|
| A类 | 明盘 | 每月一次 | 需复盘 |
| B类 | 明盘 | 每季一次 | 可选 |
| C类 | 盲盘/抽盘 | 半年一次 | 不需要 |

### 5.2 盘点单创建支持ABC筛选

```
创建盘点单时可以：
- 只盘点A类商品
- 按ABC分类生成盘点任务（A类优先）
- 不同ABC分类使用不同盘点模板
```

---

## 六、与库位推荐联动

### 6.1 入库上架推荐

```
A类商品 → 优先推荐靠近出口/通道的库位（拣货路径短）
B类商品 → 中间区域库位
C类商品 → 远端/高层库位

贯通式货架：
- A类商品 → 放浅层位置（深度号小，存取方便）
- C类商品 → 放深层位置（深度号大）
```

### 6.2 库位评分逻辑

```java
/**
 * 库位推荐评分（结合ABC分类）
 */
public int calculateLocationScore(BaseLocation location, BaseProduct product) {
    int score = 0;

    // 基础评分：库位状态、容量匹配等
    score += 基础评分;

    // ABC分类加分
    String abc = product.getAbcCategory();
    int layer = location.getLayerNum();

    if ("A".equals(abc)) {
        // A类商品优先低层（1-3层）
        if (layer <= 3) score += 20;
        else if (layer <= 5) score += 10;
    } else if ("B".equals(abc)) {
        // B类商品中等层
        if (layer >= 2 && layer <= 4) score += 15;
    } else {
        // C类商品高层也可以
        score += 5;
    }

    return score;
}
```

---

## 七、开发计划

| 阶段 | 功能 | 依赖 |
|------|------|------|
| **阶段一** | 商品表增加ABC字段，支持手动设置 | 无 |
| **阶段二** | 自动计算ABC分类（基于出库频次） | 出库模块 |
| **阶段三** | 盘点功能联动（按ABC筛选/差异化策略） | 盘点模块 |
| **阶段四** | 库位推荐联动 | 上架模块优化 |

---

## 八、API设计

### 8.1 手动设置ABC分类

```
PUT /api/v1/product/{id}/abc
Request:
{
    "abcCategory": "A"
}

Response:
{
    "success": true,
    "message": "ABC分类已更新"
}
```

### 8.2 查询ABC分类统计

```
GET /api/v1/inventory/abc-stats
Response:
{
    "success": true,
    "data": {
        "a": { "count": 120, "percent": 20 },
        "b": { "count": 180, "percent": 30 },
        "c": { "count": 300, "percent": 50 }
    }
}
```

### 8.3 触发ABC分类计算

```
POST /api/v1/inventory/abc-calculate
Response:
{
    "success": true,
    "message": "ABC分类计算完成",
    "data": {
        "updatedCount": 600
    }
}
```

---

## 九、注意事项

1. **新商品处理**：默认设为B类，首次出库后参与自动计算
2. **手动调整优先**：手动设置的分类不被自动计算覆盖
3. **历史记录**：记录分类变更历史，便于追溯
4. **多仓库场景**：ABC分类可按仓库分别计算（后续扩展）

---

*文档创建于 2026-04-29*
