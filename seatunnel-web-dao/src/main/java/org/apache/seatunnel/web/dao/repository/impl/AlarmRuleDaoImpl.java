package org.apache.seatunnel.web.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import org.apache.seatunnel.web.dao.entity.AlarmRuleEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmRuleMapper;
import org.apache.seatunnel.web.dao.repository.AlarmRuleDao;
import org.apache.seatunnel.web.dao.repository.BaseDao;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AlarmRuleDaoImpl extends BaseDao<AlarmRuleEntity, AlarmRuleMapper> implements AlarmRuleDao {

    public AlarmRuleDaoImpl(@NonNull AlarmRuleMapper alarmRuleMapper) {
        super(alarmRuleMapper);
    }

    @Override
    public List<AlarmRuleEntity> listEnabled() {
        LambdaQueryWrapper<AlarmRuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlarmRuleEntity::getEnabled, 1);
        return mybatisMapper.selectList(wrapper);
    }
}
