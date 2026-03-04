package org.apache.seatunnel.admin.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.communal.bean.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


/**
 * login controller
 */
@RestController
@RequestMapping("")
public class SeaTunnelLoginController {

//    @Autowired
//    private SessionService sessionService;
//
//    @Autowired
//    private Authenticator authenticator;
//
//    /**
//     * login
//     *
//     * @param userName     user name
//     * @param userPassword user password
//     * @param request      request
//     * @param response     response
//     * @return login result
//     */
//
//    @PostMapping(value = "/login")
//    public Result login(@RequestParam(value = "userName") String userName,
//                        @RequestParam(value = "userPassword") String userPassword,
//                        HttpServletRequest request,
//                        HttpServletResponse response) {
//        // user name check
//        if (StringUtils.isEmpty(userName)) {
//            throw new RuntimeException("user name is null");
//        }
//
//        // verify username and password
//        Result<Map<String, String>> result = authenticator.authenticate(userName, userPassword, ip);
//        if (result.getCode() != Status.SUCCESS.getCode()) {
//            return result;
//        }
//
//        response.setStatus(HttpStatus.SC_OK);
//        Map<String, String> cookieMap = result.getData();
//        for (Map.Entry<String, String> cookieEntry : cookieMap.entrySet()) {
//            Cookie cookie = new Cookie(cookieEntry.getKey(), cookieEntry.getValue());
//            cookie.setHttpOnly(true);
//            response.addCookie(cookie);
//        }
//
//        return result;
//    }

}
