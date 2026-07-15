package org.apache.seatunnel.web.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.service.AlarmChannelService;
import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleChannelEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmChannelMapper;
import org.apache.seatunnel.web.dao.mapper.AlarmRuleChannelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AlarmChannelServiceImpl implements AlarmChannelService {

    @Resource
    private AlarmChannelMapper alarmChannelMapper;

    @Resource
    private AlarmRuleChannelMapper alarmRuleChannelMapper;

    @Override
    public AlarmChannelEntity getById(Long id) {
        return alarmChannelMapper.selectById(id);
    }

    @Override
    public List<AlarmChannelEntity> list() {
        return alarmChannelMapper.selectList(null);
    }

    @Override
    public List<AlarmChannelEntity> listEnabled() {
        LambdaQueryWrapper<AlarmChannelEntity> w = new LambdaQueryWrapper<>();
        w.eq(AlarmChannelEntity::getEnabled, 1);
        return alarmChannelMapper.selectList(w);
    }

    @Override
    public Long create(AlarmChannelEntity entity) {
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        entity.initInsert();
        alarmChannelMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(AlarmChannelEntity entity) {
        if (entity.getId() == null) {
            return false;
        }
        entity.setUpdateTime(new Date());
        return alarmChannelMapper.updateById(entity) > 0;
    }

    /**
     * Delete a channel and cascade-clean any rule-channel links that reference
     * it, preventing orphaned associations that would cause rules to match a
     * non-existent channel.
     */
    @Override
    @Transactional
    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        // Remove all rule-channel links referencing this channel
        LambdaQueryWrapper<AlarmRuleChannelEntity> linkWrapper = new LambdaQueryWrapper<>();
        linkWrapper.eq(AlarmRuleChannelEntity::getChannelId, id);
        alarmRuleChannelMapper.delete(linkWrapper);
        return alarmChannelMapper.deleteById(id) > 0;
    }
}
