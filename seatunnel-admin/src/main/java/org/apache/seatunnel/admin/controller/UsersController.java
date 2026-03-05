package org.apache.seatunnel.admin.controller;


import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.seatunnel.admin.aspect.AccessLogAnnotation;
import org.apache.seatunnel.admin.service.SessionService;
import org.apache.seatunnel.admin.service.UsersService;
import org.apache.seatunnel.communal.bean.dto.UserDTO;
import org.apache.seatunnel.communal.bean.entity.Result;
import org.apache.seatunnel.communal.bean.po.User;
import org.apache.seatunnel.communal.constant.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


/**
 * users controller
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    @Resource
    private UsersService usersService;

    @Resource
    private SessionService sessionService;

    /**
     * get user info
     *
     * @param loginUser login user
     * @return user info
     */
    @GetMapping(value = "/get-user-info")
    @ResponseStatus(HttpStatus.OK)
    @AccessLogAnnotation
    public Result<User> getUserInfo(@RequestAttribute(value = Constant.SESSION_USER) User loginUser) {
        return Result.buildSuc(usersService.getUserInfo(loginUser));

    }

    @GetMapping("/currentUser")
    public Result<UserDTO> currentUser(HttpServletRequest request) {

        User loginUser = (User) request.getAttribute(Constant.SESSION_USER);

        if (loginUser == null) {
            var session = sessionService.getSession(request);
            if (session == null) {
                return Result.buildFailure("NOT_LOGIN");
            }
            loginUser = usersService.getById(session.getUserId());
        }

        if (loginUser == null) {
            return Result.buildFailure("NOT_LOGIN");
        }

        UserDTO dto = new UserDTO();
        dto.setUserName(loginUser.getUserName());
        return Result.buildSuc(dto);
    }
}
