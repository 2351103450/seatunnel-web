package org.apache.seatunnel.web.api.service;

import lombok.Data;
import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;

import java.util.List;

public interface AlarmChannelService {

    AlarmChannelEntity getById(Long id);

    List<AlarmChannelEntity> list();

    List<AlarmChannelEntity> listEnabled();

    Long create(AlarmChannelEntity entity);

    boolean update(AlarmChannelEntity entity);

    boolean delete(Long id);

    TestChannelResult testChannel(String channelType, String configJson);

    @Data
    class TestChannelResult {
        private boolean success;
        private String message;
    }
}
