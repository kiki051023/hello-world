package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /** 标记某人全部通知已读 */
    @Update("UPDATE sys_notification SET is_read = 1 WHERE receiver_id = #{receiverId} AND is_read = 0")
    int markAllRead(@Param("receiverId") Long receiverId);
}