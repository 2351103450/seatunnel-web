package org.apache.seatunnel.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.admin.controller.BaseController;
import org.apache.seatunnel.admin.dao.SessionMapper;
import org.apache.seatunnel.admin.service.SessionService;
import org.apache.seatunnel.communal.bean.po.Session;
import org.apache.seatunnel.communal.bean.po.User;
import org.apache.seatunnel.communal.constant.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session>
        implements SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    @Override
    public Session getSession(HttpServletRequest request) {
        String sessionId = request.getHeader(Constant.SESSION_ID);

        if (StringUtils.isBlank(sessionId)) {
            Cookie cookie = WebUtils.getCookie(request, Constant.SESSION_ID);

            if (cookie != null) {
                sessionId = cookie.getValue();
            }
        }

        if (StringUtils.isBlank(sessionId)) {
            return null;
        }

        String ip = BaseController.getClientIpAddress(request);
        logger.debug("get session: {}, ip: {}", sessionId, ip);

        return getById(sessionId);
    }

    @Override
    @Transactional
    public String createSession(User user, String ip) {
        Session session = null;

        LambdaQueryWrapper<Session> sessionLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sessionLambdaQueryWrapper.eq(Session::getUserId, user.getId());
        // logined
        List<Session> sessionList = getBaseMapper().selectList(sessionLambdaQueryWrapper);

        Date now = new Date();

        /*
         * if you have logged in and are still valid, return directly
         */
        if (CollectionUtils.isNotEmpty(sessionList)) {
            // is session list greater 1 ， delete other ，get one
            if (sessionList.size() > 1) {
                for (int i = 1; i < sessionList.size(); i++) {
                    removeById(sessionList.get(i).getId());
                }
            }
            session = sessionList.get(0);
            if (now.getTime() - session.getLastLoginTime().getTime() <= Constant.SESSION_TIME_OUT * 1000) {
                /*
                 * updateProcessInstance the latest login time
                 */
                session.setLastLoginTime(now);
                getBaseMapper().updateById(session);

                return session.getId();

            } else {
                /*
                 * session expired, then delete this session first
                 */
                removeById(session.getId());
            }
        }

        // assign new session
        session = new Session();

        session.setId(UUID.randomUUID().toString());
        session.setIp(ip);
        session.setUserId(user.getId());
        session.setLastLoginTime(now);

        save(session);

        return session.getId();
    }

    @Override
    public void signOut(String ip, User loginUser) {
        try {
            /*
             * query session by user id and ip
             */
            LambdaQueryWrapper<Session> sessionLambdaQueryWrapper = new LambdaQueryWrapper<>();
            sessionLambdaQueryWrapper.eq(Session::getUserId, loginUser.getId());
            sessionLambdaQueryWrapper.eq(Session::getIp, ip);
            Session session = getBaseMapper().selectOne(sessionLambdaQueryWrapper);

            //delete session
            removeById(session.getId());
        } catch (Exception e) {
            logger.warn("userId : {} , ip : {} , find more one session", loginUser.getId(), ip);
        }
    }
}
