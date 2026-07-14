package org.apache.seatunnel.web.api.alarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.seatunnel.web.dao.entity.AlarmRuleChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmRuleChannelMapper;
import org.apache.seatunnel.web.dao.mapper.AlarmRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class AlarmRuleService {

    @Resource
    private AlarmRuleMapper alarmRuleMapper;

    @Resource
    private AlarmRuleChannelMapper alarmRuleChannelMapper;

    public AlarmRuleEntity getById(Long id) {
        return alarmRuleMapper.selectById(id);
    }

    public List<AlarmRuleEntity> list() {
        return alarmRuleMapper.selectList(null);
    }

    @Transactional
    public Long create(AlarmRuleCommand command) {
        AlarmRuleEntity entity = toEntity(command);
        entity.setId(null);
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        alarmRuleMapper.insert(entity);
        relinkChannels(entity.getId(), command.getChannelIds());
        return entity.getId();
    }

    @Transactional
    public boolean update(AlarmRuleCommand command) {
        if (command.getId() == null) {
            return false;
        }
        AlarmRuleEntity entity = toEntity(command);
        entity.setUpdateTime(new Date());
        boolean updated = alarmRuleMapper.updateById(entity) > 0;
        if (updated) {
            relinkChannels(entity.getId(), command.getChannelIds());
        }
        return updated;
    }

    @Transactional
    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        LambdaQueryWrapper<AlarmRuleChannelEntity> w = new LambdaQueryWrapper<>();
        w.eq(AlarmRuleChannelEntity::getRuleId, id);
        alarmRuleChannelMapper.delete(w);
        return alarmRuleMapper.deleteById(id) > 0;
    }

    public List<AlarmRuleChannelEntity> listChannels(Long ruleId) {
        if (ruleId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<AlarmRuleChannelEntity> w = new LambdaQueryWrapper<>();
        w.eq(AlarmRuleChannelEntity::getRuleId, ruleId);
        return alarmRuleChannelMapper.selectList(w);
    }

    private void relinkChannels(Long ruleId, List<Long> channelIds) {
        LambdaQueryWrapper<AlarmRuleChannelEntity> w = new LambdaQueryWrapper<>();
        w.eq(AlarmRuleChannelEntity::getRuleId, ruleId);
        alarmRuleChannelMapper.delete(w);
        if (channelIds == null || channelIds.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (Long channelId : channelIds) {
            if (channelId == null) {
                continue;
            }
            AlarmRuleChannelEntity link = new AlarmRuleChannelEntity();
            link.setRuleId(ruleId);
            link.setChannelId(channelId);
            link.setCreateTime(now);
            alarmRuleChannelMapper.insert(link);
        }
    }

    private AlarmRuleEntity toEntity(AlarmRuleCommand command) {
        return AlarmRuleEntity.builder()
                .id(command.getId())
                .name(command.getName())
                .jobDefinitionId(command.getJobDefinitionId())
                .triggerStatuses(command.getTriggerStatuses())
                .excludes(command.getExcludes())
                .severity(command.getSeverity())
                .enabled(command.getEnabled())
                .description(command.getDescription())
                .build();
    }

    @Data
    public static class AlarmRuleCommand {
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
