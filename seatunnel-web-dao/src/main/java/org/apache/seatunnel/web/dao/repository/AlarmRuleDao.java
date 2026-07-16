package org.apache.seatunnel.web.dao.repository;

import org.apache.seatunnel.web.dao.entity.AlarmRuleEntity;

import java.util.List;

public interface AlarmRuleDao extends IDao<AlarmRuleEntity> {

    List<AlarmRuleEntity> listEnabled();
}
