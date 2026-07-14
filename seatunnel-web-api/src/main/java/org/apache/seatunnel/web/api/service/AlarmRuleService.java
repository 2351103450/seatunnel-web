package org.apache.seatunnel.web.api.service;

import lombok.Data;
import org.apache.seatunnel.web.dao.entity.AlarmRuleChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleEntity;

import java.util.List;

public interface AlarmRuleService {

    AlarmRuleEntity getById(Long id);

    List<AlarmRuleEntity> list();

    Long create(AlarmRuleCommand command);

    boolean update(AlarmRuleCommand command);

    boolean delete(Long id);

    List<AlarmRuleChannelEntity> listChannels(Long ruleId);

    @Data
    class AlarmRuleCommand {
        private Long id;
        private String name;
        private Long jobDefinitionId;
        private String triggerStatuses;
        private String excludes;
        private String severity;
        private Integer enabled;
        private String description;
        private List<Long> channelIds;
    }
}
