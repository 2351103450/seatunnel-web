package org.apache.seatunnel.web.dao.repository;

import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;

import java.util.Collection;
import java.util.List;

public interface AlarmChannelDao extends IDao<AlarmChannelEntity> {

    List<AlarmChannelEntity> listEnabled();

    List<AlarmChannelEntity> listEnabledByIds(Collection<Long> ids);
}
