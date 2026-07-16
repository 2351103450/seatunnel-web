package org.apache.seatunnel.web.dao.repository;

import org.apache.seatunnel.web.dao.entity.AlarmRuleChannelEntity;

import java.util.Collection;
import java.util.List;

public interface AlarmRuleChannelDao extends IDao<AlarmRuleChannelEntity> {

    List<AlarmRuleChannelEntity> listByRuleId(Long ruleId);

    List<AlarmRuleChannelEntity> listByRuleIds(Collection<Long> ruleIds);

    void deleteByRuleId(Long ruleId);

    void deleteByChannelId(Long channelId);
}
