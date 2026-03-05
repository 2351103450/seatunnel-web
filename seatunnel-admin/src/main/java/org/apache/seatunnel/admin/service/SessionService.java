package org.apache.seatunnel.admin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.seatunnel.communal.bean.po.Session;
import org.apache.seatunnel.communal.bean.po.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * session service
 */
public interface SessionService extends IService<Session> {

    /**
     * get user session from request
     *
     * @param request request
     * @return session
     */
    Session getSession(HttpServletRequest request);

    /**
     * create session
     *
     * @param user user
     * @param ip ip
     * @return session string
     */
    String createSession(User user, String ip);

    /**
     * sign out
     * remove ip restrictions
     *
     * @param ip   no use
     * @param loginUser login user
     */
    void signOut(String ip, User loginUser);
}
