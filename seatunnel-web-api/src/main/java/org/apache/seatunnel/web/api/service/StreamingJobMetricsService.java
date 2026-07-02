package org.apache.seatunnel.web.api.service;

import org.apache.seatunnel.web.api.metrics.streaming.model.StreamingParsedJobMetrics;
import org.apache.seatunnel.web.spi.bean.vo.StreamingMetricsSnapshotVO;
import org.apache.seatunnel.web.spi.bean.vo.StreamingMetricsTrendItemVO;
import org.apache.seatunnel.web.spi.bean.vo.StreamingMetricsTrendVO;
import org.apache.seatunnel.web.spi.bean.vo.StreamingTableMetricsVO;

import java.util.List;

public interface StreamingJobMetricsService {

    StreamingParsedJobMetrics getRealtimeMetricsFromEngine(Long clientId, String engineJobId);

    void saveSnapshot(Long jobInstanceId,
                      Long jobDefinitionId,
                      Long clientId,
                      String engineJobId,
                      StreamingParsedJobMetrics parsed);

    StreamingMetricsSnapshotVO latest(Long instanceId);

    StreamingMetricsTrendVO trend(Long instanceId,
                                  Long startTimeMs,
                                  Long endTimeMs,
                                  String granularity);

    List<StreamingMetricsTrendItemVO> recentTrend(Long instanceId, Integer limit);

    List<StreamingTableMetricsVO> listLatestTableMetrics(Long instanceId);

    void deleteByInstanceId(Long instanceId);

    void deleteByDefinitionId(Long definitionId);

    void deleteExpired(Long retentionDays);
}