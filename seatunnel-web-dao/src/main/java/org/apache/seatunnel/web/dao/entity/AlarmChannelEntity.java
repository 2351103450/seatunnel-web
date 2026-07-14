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
 * A configured alarm channel instance (e.g. a specific webhook endpoint).
 * The {@code channelType} is the SPI key used to resolve an
 * {@link org.apache.seatunnel.plugin.alarm.api.AlarmChannel} implementation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_seatunnel_web_alarm_channel")
public class AlarmChannelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** SPI key, e.g. WEBHOOK / DINGTALK. */
    private String channelType;

    /** Channel config as JSON string, parsed at delivery time. */
    private String configJson;

    /** 0 disabled, 1 enabled. */
    private Integer enabled;

    private String description;

    private Date createTime;

    private Date updateTime;
}
