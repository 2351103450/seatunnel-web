package org.apache.seatunnel.web.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.seatunnel.web.dao.entity.AlarmRecordEntity;

public interface AlarmRecordDao extends IDao<AlarmRecordEntity> {

    IPage<AlarmRecordEntity> page(int pageNo, int pageSize, Long jobInstanceId,
                                  String channelType, String severity, Integer success);
}
