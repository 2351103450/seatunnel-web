package org.apache.seatunnel.web.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.service.AlarmChannelService;
import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmChannelMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AlarmChannelServiceImpl implements AlarmChannelService {

    @Resource
    private AlarmChannelMapper alarmChannelMapper;

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
        entity.setId(null);
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
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

    @Override
    public boolean delete(Long id) {
        return alarmChannelMapper.deleteById(id) > 0;
    }
}
