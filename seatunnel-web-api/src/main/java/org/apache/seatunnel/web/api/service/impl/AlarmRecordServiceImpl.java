package org.apache.seatunnel.web.api.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.apache.seatunnel.web.api.service.AlarmRecordService;
import org.apache.seatunnel.web.dao.entity.AlarmRecordEntity;
import org.apache.seatunnel.web.dao.repository.AlarmRecordDao;
import org.springframework.stereotype.Service;

@Service
public class AlarmRecordServiceImpl implements AlarmRecordService {

    @Resource
    private AlarmRecordDao alarmRecordDao;

    @Override
    public IPage<AlarmRecordEntity> page(int pageNo, int pageSize, Long jobInstanceId) {
        return page(pageNo, pageSize, jobInstanceId, null, null, null);
    }

    @Override
    public IPage<AlarmRecordEntity> page(int pageNo, int pageSize, Long jobInstanceId,
                                           String channelType, String severity, Integer success) {
        return alarmRecordDao.page(pageNo, pageSize, jobInstanceId, channelType, severity, success);
    }

    @Override
    public void save(AlarmRecordEntity entity) {
        if (entity.getSentTime() == null) {
            entity.setSentTime(new java.util.Date());
        }
        entity.initInsert();
        alarmRecordDao.insert(entity);
    }
}
