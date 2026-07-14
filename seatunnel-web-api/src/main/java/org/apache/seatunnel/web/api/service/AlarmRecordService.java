package org.apache.seatunnel.web.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.seatunnel.web.dao.entity.AlarmRecordEntity;

public interface AlarmRecordService {

    IPage<AlarmRecordEntity> page(int pageNo, int pageSize, Long jobInstanceId);
}
