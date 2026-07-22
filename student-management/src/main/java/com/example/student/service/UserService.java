package com.example.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.student.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    Map<String, Object> login(String username, String password);

    User getByUsername(String username);
}
