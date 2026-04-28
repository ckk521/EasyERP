package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.system.entity.BaseCategory;
import com.wms.system.repository.BaseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final BaseCategoryRepository categoryRepository;

    public List<Map<String, Object>> getCategoryTree() {
        List<BaseCategory> allCategories = categoryRepository.selectList(
            new LambdaQueryWrapper<BaseCategory>().eq(BaseCategory::getStatus, 1).orderByAsc(BaseCategory::getSortOrder)
        );

        return buildTree(allCategories, null);
    }

    public List<Map<String, Object>> getCategoriesByParentId(Long parentId) {
        LambdaQueryWrapper<BaseCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseCategory::getParentId, parentId);
        wrapper.eq(BaseCategory::getStatus, 1);
        wrapper.orderByAsc(BaseCategory::getSortOrder);

        List<BaseCategory> categories = categoryRepository.selectList(wrapper);
        return categories.stream().map(this::toMap).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTree(List<BaseCategory> allCategories, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (BaseCategory category : allCategories) {
            Long catParentId = category.getParentId();
            boolean isMatch = (parentId == null && catParentId == null) ||
                              (parentId != null && parentId.equals(catParentId));

            if (isMatch) {
                Map<String, Object> node = toMap(category);
                List<Map<String, Object>> children = buildTree(allCategories, category.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                result.add(node);
            }
        }

        return result;
    }

    private Map<String, Object> toMap(BaseCategory category) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", category.getId());
        map.put("code", category.getCode());
        map.put("name", category.getName());
        map.put("parentId", category.getParentId());
        map.put("level", category.getLevel());
        map.put("sortOrder", category.getSortOrder());
        return map;
    }
}
