package org.apache.seatunnel.communal.bean.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.seatunnel.communal.enums.UserType;

import java.util.Date;

@Data
public class UserDTO {

    private Integer id;

    private String userName;

    private String userPassword;

    private String email;

    private String phone;

    private UserType userType;

    private int state;

    private String timeZone;

    private Date createTime;

    private Date updateTime;

}
