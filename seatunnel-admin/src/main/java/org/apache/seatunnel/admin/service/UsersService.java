package org.apache.seatunnel.admin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.seatunnel.communal.bean.po.SeatunnelStreamJobDefinitionPO;
import org.apache.seatunnel.communal.bean.po.User;

/**
 * users service
 */
public interface UsersService extends IService<User> {


    User queryUser(String userId, String password);
}
