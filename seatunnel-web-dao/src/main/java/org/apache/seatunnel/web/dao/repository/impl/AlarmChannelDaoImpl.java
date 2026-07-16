package org.apache.seatunnel.web.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmChannelMapper;
import org.apache.seatunnel.web.dao.repository.AlarmChannelDao;
import org.apache.seatunnel.web.dao.repository.BaseDao;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class AlarmChannelDaoImpl extends BaseDao<AlarmChannelEntity, AlarmChannelMapper>
        implements AlarmChannelDao {

    public AlarmChannelDaoImpl(@NonNull AlarmChannelMapper alarmChannelMapper) {
        super(alarmChannelMapper);
    }

    @Override
    public List<AlarmChannelEntity> listEnabled() {
        LambdaQueryWrapper<AlarmChannelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlarmChannelEntity::getEnabled, 1);
        return mybatisMapper.selectList(wrapper);
    }

    @Override
    public List<AlarmChannelEntity> listEnabledByIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<AlarmChannelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AlarmChannelEntity::getId, ids)
                .eq(AlarmChannelEntity::getEnabled, 1);
        return mybatisMapper.selectList(wrapper);
    }
}
