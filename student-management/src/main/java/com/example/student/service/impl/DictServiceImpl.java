package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.DictData;
import com.example.student.entity.DictType;
import com.example.student.exception.BusinessException;
import com.example.student.mapper.DictDataMapper;
import com.example.student.mapper.DictTypeMapper;
import com.example.student.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DictServiceImpl extends ServiceImpl<DictTypeMapper, DictType> implements DictService {

    /** 字典缓存 key 前缀（变更时 evictByPrefix 失效） */
    private static final String CACHE_KEY = "dict:enabled:";
    /** 字典缓存 TTL：5 分钟（管理端改动即时 evict，TTL 只是兜底） */
    private static final long CACHE_TTL = 300;

    @Autowired
    private DictDataMapper dictDataMapper;

    @Autowired
    private com.example.student.common.DistributedCacheService cache;

    /** 缓存命中日志（M2-2 验收：第二次查库为零） */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DictServiceImpl.class);

    private void evictDictCache() {
        cache.evictByPrefix(CACHE_KEY);
    }

    @Override
    public IPage<DictType> getTypePage(Long current, Long size, String keyword) {
        LambdaQueryWrapper<DictType> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(DictType::getDictType, keyword).or().like(DictType::getDictName, keyword);
        }
        w.orderByAsc(DictType::getId);
        return page(new Page<>(current, size), w);
    }

    @Override
    public List<DictData> listEnabledData(String dictType) {
        Object cached = cache.getOrLoad(CACHE_KEY + dictType, CACHE_TTL, k -> {
            log.debug("dict cache miss: {}", dictType);
            return dictDataMapper.selectList(new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getDictType, dictType)
                    .eq(DictData::getStatus, 1)
                    .orderByAsc(DictData::getSort));
        });
        // ponytail: Redis 往返把 List<DictData> 反序列化成 List<Map>，
        // 前端只消费 dictValue/dictLabel 字段，结构同构，不再转回实体。
        return (List<DictData>) cached;
    }

    @Override
    public IPage<DictData> getDataPage(Long current, Long size, String dictType) {
        return dictDataMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<DictData>()
                        .eq(DictData::getDictType, dictType)
                        .orderByAsc(DictData::getSort));
    }

    @Override
    public void addType(DictType type) {
        if (count(new LambdaQueryWrapper<DictType>().eq(DictType::getDictType, type.getDictType())) > 0) {
            throw new BusinessException("字典类型已存在: " + type.getDictType());
        }
        save(type);
    }

    @Override
    public void updateType(DictType type) {
        DictType old = getById(type.getId());
        if (old == null) {
            throw new BusinessException("字典类型不存在");
        }
        // dictType 不可改，避免既有数据失联
        type.setDictType(old.getDictType());
        updateById(type);
        evictDictCache();
    }

    @Override
    public void deleteType(Long id) {
        DictType old = getById(id);
        if (old == null) {
            throw new BusinessException("字典类型不存在");
        }
        Long dataCount = dictDataMapper.selectCount(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictType, old.getDictType()));
        if (dataCount > 0) {
            throw new BusinessException("该类型下存在 " + dataCount + " 条数据，不能删除");
        }
        removeById(id);
        evictDictCache();
    }

    @Override
    public void addData(DictData data) {
        if (dictDataMapper.selectCount(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictType, data.getDictType())
                .eq(DictData::getDictValue, data.getDictValue())) > 0) {
            throw new BusinessException("同类型下字典值已存在: " + data.getDictValue());
        }
        dictDataMapper.insert(data);
        evictDictCache();
    }

    @Override
    public void updateData(DictData data) {
        DictData old = dictDataMapper.selectById(data.getId());
        if (old == null) {
            throw new BusinessException("字典数据不存在");
        }
        // 类型+值组合不允许改成与已有记录冲突的值
        if (!old.getDictValue().equals(data.getDictValue())
                && dictDataMapper.selectCount(new LambdaQueryWrapper<DictData>()
                        .eq(DictData::getDictType, old.getDictType())
                        .eq(DictData::getDictValue, data.getDictValue())) > 0) {
            throw new BusinessException("同类型下字典值已存在: " + data.getDictValue());
        }
        data.setDictType(old.getDictType());
        dictDataMapper.updateById(data);
        evictDictCache();
    }

    @Override
    public void deleteData(Long id) {
        dictDataMapper.deleteById(id);
        evictDictCache();
    }

    @Override
    public String translate(String dictType, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        DictData d = dictDataMapper.selectOne(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictType, dictType)
                .eq(DictData::getDictValue, value)
                .last("LIMIT 1"));
        return d == null ? value : d.getDictLabel();
    }
}
