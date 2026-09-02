package com.example.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.student.entity.DormRoom;
import com.example.student.mapper.DormRoomMapper;
import com.example.student.service.DormRoomService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DormRoomServiceImpl extends ServiceImpl<DormRoomMapper, DormRoom> implements DormRoomService {

    @Override
    public IPage<DormRoom> getPage(Long current, Long size, String keyword) {
        LambdaQueryWrapper<DormRoom> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(DormRoom::getBuilding, keyword);
        }
        w.orderByDesc(DormRoom::getId);
        return page(new Page<>(current, size), w);
    }
}
