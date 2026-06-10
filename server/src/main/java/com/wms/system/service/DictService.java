package com.wms.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.system.entity.SysDict;
import com.wms.system.repository.SysDictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictRepository sysDictRepository;

    /**
     * 查询指定类型的字典列表
     */
    public List<Map<String, Object>> getDictByType(String type) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getDictType, type)
               .eq(SysDict::getStatus, 1)
               .orderByAsc(SysDict::getSortOrder);

        List<SysDict> dicts = sysDictRepository.selectList(wrapper);

        return dicts.stream().map(dict -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", dict.getDictCode());
            item.put("name", dict.getDictValue());
            item.put("isSystem", dict.getIsSystem());
            item.put("sortOrder", dict.getSortOrder());
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 查询所有字典类型
     */
    public List<String> getDictTypes() {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysDict::getDictType)
               .groupBy(SysDict::getDictType);

        List<SysDict> dicts = sysDictRepository.selectList(wrapper);
        return dicts.stream().map(SysDict::getDictType).collect(Collectors.toList());
    }

    /**
     * 查询所有字典（按类型分组）
     */
    public Map<String, List<Map<String, Object>>> getAllDicts() {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getStatus, 1)
               .orderByAsc(SysDict::getDictType)
               .orderByAsc(SysDict::getSortOrder);

        List<SysDict> allDicts = sysDictRepository.selectList(wrapper);

        return allDicts.stream().collect(
            Collectors.groupingBy(
                SysDict::getDictType,
                Collectors.mapping(dict -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", dict.getDictCode());
                    item.put("name", dict.getDictValue());
                    item.put("isSystem", dict.getIsSystem());
                    return item;
                }, Collectors.toList())
            )
        );
    }
}