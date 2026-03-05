package org.apache.seatunnel.admin.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.admin.aspect.AccessLogAnnotation;
import org.apache.seatunnel.admin.security.Authenticator;
import org.apache.seatunnel.admin.service.SessionService;
import org.apache.seatunnel.communal.bean.entity.Result;
import org.apache.seatunnel.communal.bean.po.User;
import org.apache.seatunnel.communal.constant.Constant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


/**
 * login controller
 */
@RestController
@RequestMapping("")
public class SeaTunnelLoginController extends BaseController {

    @Resource
    private SessionService sessionService;

    @Resource
    private Authenticator authenticator;

    /**
     * login
     *
     * @param userName     user name
     * @param userPassword user password
     * @param request      request
     * @param response     response
     * @return login result
     */

    @PostMapping(value = "/login")
    @AccessLogAnnotation(ignoreRequestArgs = {"userPassword", "request", "response"})
    public Result<Boolean> login(@RequestParam(value = "userName") String userName,
                                 @RequestParam(value = "userPassword") String userPassword,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        // user name check
        if (StringUtils.isEmpty(userName)) {
            throw new RuntimeException("user name is null");
        }

        String ip = getClientIpAddress(request);

        // verify username and password
        Map<String, String> cookieMap = authenticator.authenticate(userName, userPassword, ip);

        for (Map.Entry<String, String> cookieEntry : cookieMap.entrySet()) {
            Cookie cookie = new Cookie(cookieEntry.getKey(), cookieEntry.getValue());
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }

        return Result.buildSuc();
    }

    /**
     * sign out
     *
     * @param loginUser login user
     * @param request   request
     * @return sign out result
     */
    @PostMapping(value = "/signOut")
    @AccessLogAnnotation(ignoreRequestArgs = {"loginUser", "request"})
    public Result<Boolean> signOut(User loginUser,
                                   HttpServletRequest request) {
        String ip = getClientIpAddress(request);
        sessionService.signOut(ip, loginUser);
        // clear session
        request.removeAttribute(Constant.SESSION_USER);
        return Result.buildSuc();
    }

}
