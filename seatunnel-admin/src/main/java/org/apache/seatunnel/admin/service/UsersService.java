package org.apache.seatunnel.admin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.seatunnel.communal.bean.po.UserPO;

/**
 * users service
 */
public interface UsersService extends IService<UserPO> {


    UserPO queryUser(String userId, String password);

    UserPO getUserInfo(UserPO loginUserPO);
}
