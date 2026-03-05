package org.apache.seatunnel.admin.security.impl;

import jakarta.annotation.Resource;
import org.apache.seatunnel.admin.security.Authenticator;
import org.apache.seatunnel.admin.security.SecurityConfig;
import org.apache.seatunnel.admin.service.SessionService;
import org.apache.seatunnel.admin.service.UsersService;
import org.apache.seatunnel.communal.bean.entity.Result;
import org.apache.seatunnel.communal.bean.po.User;
import org.apache.seatunnel.communal.constant.Constant;
import org.apache.seatunnel.communal.enums.Flag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAuthenticator implements Authenticator {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAuthenticator.class);

    @Resource
    protected UsersService userService;

    @Resource
    private SessionService sessionService;

    @Resource
    private SecurityConfig securityConfig;

    @Override
    public Map<String, String> authenticate(String userId, String password, String extra) {
        User user = login(userId, password, extra);
        if (user == null) {
            throw new RuntimeException("user name or password error");
        }

        if (user.getState() == Flag.NO.ordinal()) {
            throw new RuntimeException("The current user is disabled");
        }

        // create session
        String sessionId = sessionService.createSession(user, extra);
        if (sessionId == null) {
            throw new RuntimeException("create session failed!");
        }

        logger.info("sessionId : {}", sessionId);
        Map<String, String> data = new HashMap<>();
        data.put(Constant.SESSION_ID, sessionId);
        data.put(Constant.SECURITY_CONFIG_TYPE, securityConfig.getType());
        return data;

    }

    /**
     * user login and return user in db
     *
     * @param userId   user identity field
     * @param password user login password
     * @param extra    extra user login field
     * @return user object in databse
     */
    public abstract User login(String userId, String password, String extra);
}
