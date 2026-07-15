package org.apache.seatunnel.web.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.service.AlarmRuleService;
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
public class AlarmRuleServiceImpl implements AlarmRuleService {

    @Resource
    private AlarmRuleMapper alarmRuleMapper;

    @Resource
    private AlarmRuleChannelMapper alarmRuleChannelMapper;

    @Override
    public AlarmRuleEntity getById(Long id) {
        return alarmRuleMapper.selectById(id);
    }

    @Override
    public List<AlarmRuleEntity> list() {
        return alarmRuleMapper.selectList(null);
    }

    @Override
    @Transactional
    public Long create(AlarmRuleCommand command) {
        AlarmRuleEntity entity = toEntity(command);
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        entity.initInsert();
        alarmRuleMapper.insert(entity);
        relinkChannels(entity.getId(), command.getChannelIds());
        return entity.getId();
    }

    @Override
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

    @Override
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

    @Override
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
        for (Long channelId : channelIds) {
            if (channelId == null) {
                continue;
            }
            AlarmRuleChannelEntity link = new AlarmRuleChannelEntity();
            link.setRuleId(ruleId);
            link.setChannelId(channelId);
            link.initInsert();
            alarmRuleChannelMapper.insert(link);
        }
    }

    private AlarmRuleEntity toEntity(AlarmRuleCommand command) {
        // id / createTime / updateTime are inherited from BaseEntity, so they are
        // not part of AlarmRuleEntity's @Builder; set id via the setter instead.
        AlarmRuleEntity entity = AlarmRuleEntity.builder()
                .name(command.getName())
                .targetJobs(command.getTargetJobs())
                .triggerStatuses(command.getTriggerStatuses())
                .excludes(command.getExcludes())
                .severity(command.getSeverity())
                .enabled(command.getEnabled())
                .description(command.getDescription())
                .build();
        entity.setId(command.getId());
        return entity;
    }
}
