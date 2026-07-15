package org.apache.seatunnel.web.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.service.AlarmRecordService;
import org.apache.seatunnel.web.dao.entity.AlarmRecordEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class AlarmRecordServiceImpl implements AlarmRecordService {

    @Resource
    private AlarmRecordMapper alarmRecordMapper;

    @Override
    public IPage<AlarmRecordEntity> page(int pageNo, int pageSize, Long jobInstanceId) {
        return page(pageNo, pageSize, jobInstanceId, null, null, null);
    }

    @Override
    public IPage<AlarmRecordEntity> page(int pageNo, int pageSize, Long jobInstanceId,
                                           String channelType, String severity, Integer success) {
        Page<AlarmRecordEntity> page = new Page<>(pageNo < 1 ? 1 : pageNo, pageSize < 1 ? 10 : pageSize);
        LambdaQueryWrapper<AlarmRecordEntity> w = new LambdaQueryWrapper<>();
        if (jobInstanceId != null) {
            w.eq(AlarmRecordEntity::getJobInstanceId, jobInstanceId);
        }
        if (channelType != null && !channelType.isBlank()) {
            w.eq(AlarmRecordEntity::getChannelType, channelType);
        }
        if (severity != null && !severity.isBlank()) {
            w.eq(AlarmRecordEntity::getSeverity, severity);
        }
        if (success != null) {
            w.eq(AlarmRecordEntity::getSuccess, success);
        }
        w.orderByDesc(AlarmRecordEntity::getCreateTime);
        return alarmRecordMapper.selectPage(page, w);
    }

    @Override
    public void save(AlarmRecordEntity entity) {
        if (entity.getSentTime() == null) {
            entity.setSentTime(new java.util.Date());
        }
        entity.initInsert();
        alarmRecordMapper.insert(entity);
    }
}
