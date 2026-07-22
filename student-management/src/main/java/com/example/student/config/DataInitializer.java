package com.example.student.config;

import com.example.student.entity.User;
import com.example.student.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
    }

    private void initAdminUser() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, "admin");
        User admin = userMapper.selectOne(wrapper);

        if (admin == null) {
            User user = new User();
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRealName("系统管理员");
            user.setRole("ADMIN");
            user.setStatus(1);
            userMapper.insert(user);
            log.info("初始化管理员账号：admin / 123456");
        } else {
            if (!passwordEncoder.matches("123456", admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode("123456"));
                userMapper.updateById(admin);
                log.info("重置管理员密码为：123456");
            } else {
                log.info("管理员账号密码正常");
            }
        }
    }
}
