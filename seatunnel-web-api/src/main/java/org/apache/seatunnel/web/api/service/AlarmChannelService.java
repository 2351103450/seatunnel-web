package org.apache.seatunnel.web.api.service;

import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;

import java.util.List;

public interface AlarmChannelService {

    AlarmChannelEntity getById(Long id);

    List<AlarmChannelEntity> list();

    List<AlarmChannelEntity> listEnabled();

    Long create(AlarmChannelEntity entity);

    boolean update(AlarmChannelEntity entity);

    boolean delete(Long id);
}
