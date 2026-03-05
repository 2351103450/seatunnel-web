package org.apache.seatunnel.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.seatunnel.admin.dao.UserMapper;
import org.apache.seatunnel.admin.service.UsersService;
import org.apache.seatunnel.admin.utils.EncryptionUtils;
import org.apache.seatunnel.communal.bean.po.User;
import org.apache.seatunnel.communal.enums.UserType;
import org.springframework.stereotype.Service;

@Service
public class UsersServiceImpl extends ServiceImpl<UserMapper, User>
        implements UsersService {

    @Override
    public User queryUser(String name, String password) {
        String md5 = EncryptionUtils.getMd5(password);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserName, name)
                .eq(User::getUserPassword, md5);
        return getBaseMapper().selectOne(wrapper);
    }

    @Override
    public User getUserInfo(User loginUser) {
        User user;
        if (loginUser.getUserType() == UserType.ADMIN_USER) {
            user = loginUser;
        } else {
            user = getById(loginUser.getId());
        }
        return user;
    }
}
