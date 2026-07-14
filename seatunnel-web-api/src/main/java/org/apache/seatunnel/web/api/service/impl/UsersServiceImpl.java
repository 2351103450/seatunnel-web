package org.apache.seatunnel.web.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.service.UsersService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.apache.seatunnel.web.common.enums.UserType;
import org.apache.seatunnel.web.dao.entity.User;
import org.apache.seatunnel.web.dao.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UsersServiceImpl implements UsersService {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Resource
    private UserMapper userMapper;

    @Override
    public User queryUser(String name, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserName, name);
        User user = userMapper.selectOne(wrapper);
        if (user == null || !PASSWORD_ENCODER.matches(password, user.getUserPassword())) {
            return null;
        }
        return user;
    }

    @Override
    public User getUserInfo(User loginUser) {
        User User;
        if (loginUser.getUserType() == UserType.ADMIN_USER) {
            User = loginUser;
        } else {
            User = userMapper.selectById(loginUser.getId());
        }
        return User;
    }

    @Override
    public User getById(int userId) {
        return userMapper.selectById(userId);
    }
}
