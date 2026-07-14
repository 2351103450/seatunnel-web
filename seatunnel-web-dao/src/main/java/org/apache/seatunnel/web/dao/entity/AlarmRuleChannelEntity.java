package org.apache.seatunnel.web.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Many-to-many link between an alarm rule and an alarm channel.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_seatunnel_web_alarm_rule_channel")
public class AlarmRuleChannelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private Long channelId;

    private Date createTime;
}
