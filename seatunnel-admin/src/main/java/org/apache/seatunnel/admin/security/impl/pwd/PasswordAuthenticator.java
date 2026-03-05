package org.apache.seatunnel.admin.security.impl.pwd;


import org.apache.seatunnel.admin.security.impl.AbstractAuthenticator;
import org.apache.seatunnel.communal.bean.po.User;

public class PasswordAuthenticator extends AbstractAuthenticator {

    @Override
    public User login(String userId, String password, String extra) {
        return userService.queryUser(userId, password);
    }
}
