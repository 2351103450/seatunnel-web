package org.apache.seatunnel.web.api.alarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.dao.entity.AlarmRecordEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class AlarmRecordService {

    @Resource
    private AlarmRecordMapper alarmRecordMapper;

    public IPage<AlarmRecordEntity> page(int pageNo, int pageSize, Long jobInstanceId) {
        Page<AlarmRecordEntity> page = new Page<>(pageNo < 1 ? 1 : pageNo, pageSize < 1 ? 10 : pageSize);
        LambdaQueryWrapper<AlarmRecordEntity> w = new LambdaQueryWrapper<>();
        if (jobInstanceId != null) {
            w.eq(AlarmRecordEntity::getJobInstanceId, jobInstanceId);
        }
        w.orderByDesc(AlarmRecordEntity::getCreateTime);
        return alarmRecordMapper.selectPage(page, w);
    }
}
