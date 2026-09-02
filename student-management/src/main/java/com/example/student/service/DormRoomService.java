package com.example.student.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.DormRoom;

public interface DormRoomService extends IService<DormRoom> {
    IPage<DormRoom> getPage(Long current, Long size, String keyword);
}
