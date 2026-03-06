package org.apache.seatunnel.admin.security.impl.pwd;


import org.apache.seatunnel.admin.security.impl.AbstractAuthenticator;
import org.apache.seatunnel.communal.bean.po.UserPO;

public class PasswordAuthenticator extends AbstractAuthenticator {

    @Override
    public UserPO login(String userId, String password, String extra) {
        return userService.queryUser(userId, password);
    }
}
