package org.apache.seatunnel.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.admin.builder.HoconConfigBuilder;
import org.apache.seatunnel.admin.dag.DagGraph;
import org.apache.seatunnel.admin.dag.StreamDagAssembler;
import org.apache.seatunnel.admin.dag.WholeSyncDagAssembler;
import org.apache.seatunnel.admin.dao.SeatunnelJobInstanceMapper;
import org.apache.seatunnel.admin.dao.SeatunnelJobMetricsMapper;
import org.apache.seatunnel.admin.service.SeatunnelBatchJobDefinitionService;
import org.apache.seatunnel.admin.service.SeatunnelJobInstanceService;
import org.apache.seatunnel.admin.thirdparty.client.SeatunnelRestClient;
import org.apache.seatunnel.admin.utils.DagUtil;
import org.apache.seatunnel.communal.bean.dto.BaseSeatunnelJobDefinitionDTO;
import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionDTO;
import org.apache.seatunnel.communal.bean.dto.SeatunnelJobInstanceDTO;
import org.apache.seatunnel.communal.bean.entity.PaginationResult;
import org.apache.seatunnel.communal.bean.po.SeatunnelJobInstancePO;
import org.apache.seatunnel.communal.bean.po.SeatunnelJobMetricsPO;
import org.apache.seatunnel.communal.bean.vo.SeatunnelBatchJobDefinitionVO;
import org.apache.seatunnel.communal.bean.vo.SeatunnelJobInstanceVO;
import org.apache.seatunnel.communal.enums.JobMode;
import org.apache.seatunnel.communal.enums.JobStatus;
import org.apache.seatunnel.communal.enums.RunMode;
import org.apache.seatunnel.communal.utils.CodeGenerateUtils;
import org.apache.seatunnel.communal.utils.ConvertUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class SeatunnelJobInstanceServiceImpl
        extends ServiceImpl<SeatunnelJobInstanceMapper, SeatunnelJobInstancePO>
        implements SeatunnelJobInstanceService {

    @Lazy
    @Resource
    private SeatunnelBatchJobDefinitionService definitionService;

    @Resource
    private SeatunnelJobMetricsMapper metricsMapper;

    @Resource
    private HoconConfigBuilder configBuilder;

    @Resource
    private SeatunnelRestClient seatunnelRestClient;

    @Value("${seatunnel.job.log-dir:logs}")
    private String baseLogDir;

    @PostConstruct
    public void init() {
        log.info("SeatunnelJobInstanceServiceImpl initialized: {}", System.identityHashCode(this));
    }

    @Override
    public SeatunnelJobInstanceVO create(Long jobDefineId, RunMode runMode) {
        log.info("Creating job instance, jobDefineId={}, runMode={}", jobDefineId, runMode);

        SeatunnelBatchJobDefinitionVO definitionVO = definitionService.selectById(jobDefineId);
        if (definitionVO == null) {
            throw new IllegalArgumentException("Job definition not found: " + jobDefineId);
        }

        SeatunnelJobInstancePO instance = buildJobInstance(definitionVO, runMode);

        boolean saved = save(instance);
        if (!saved) {
            throw new IllegalStateException("Failed to save job instance");
        }

        log.info("Job instance created successfully, instanceId={}", instance.getId());
        return ConvertUtil.sourceToTarget(instance, SeatunnelJobInstanceVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeatunnelJobInstanceVO createAndSubmit(Long jobDefineId, RunMode runMode) {
        SeatunnelBatchJobDefinitionVO definitionVO = definitionService.selectById(jobDefineId);
        if (definitionVO == null) {
            throw new IllegalArgumentException("Job definition not found: " + jobDefineId);
        }

        SeatunnelJobInstancePO instance = buildJobInstance(definitionVO, runMode);

        boolean saved = save(instance);
        if (!saved) {
            throw new IllegalStateException("Failed to save job instance");
        }

        try {
            Map<?, ?> result = seatunnelRestClient.submitJobUploadText(
                    instance.getJobConfig(),
                    "job-" + instance.getId() + ".conf"
            );

            String engineJobId = parseEngineJobId(result);

            JobStatus submitStatus = parseJobStatus(result);
            if (submitStatus == null) {
                submitStatus = JobStatus.INITIALIZING;
            }

            updateStatusAndEngineId(instance.getId(), submitStatus, engineJobId);

            log.info("Job submitted successfully, instanceId={}, engineJobId={}, status={}",
                    instance.getId(), engineJobId, submitStatus);

            return selectById(instance.getId());
        } catch (Exception e) {
            log.error("Submit job failed, instanceId={}", instance.getId(), e);
            updateStatus(instance.getId(), JobStatus.FAILED, e.getMessage());
            throw e;
        }
    }

    @Override
    public PaginationResult<SeatunnelJobInstanceVO> paging(SeatunnelJobInstanceDTO dto) {
        long pageNo = dto.getPageNo() == null || dto.getPageNo() < 1 ? 1 : dto.getPageNo();
        long pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
        Page<SeatunnelJobInstanceVO> page = new Page<>(pageNo, pageSize);
        IPage<SeatunnelJobInstanceVO> result = baseMapper.pageWithDefinition(page, dto);
        return PaginationResult.buildSuc(result.getRecords(), result);
    }

    @Override
    public String buildHoconConfig(BaseSeatunnelJobDefinitionDTO dto) {
        return buildConfig(dto.getJobDefinitionInfo(), dto);
    }

    @Override
    public String buildHoconConfigByWholeSync(BaseSeatunnelJobDefinitionDTO dto) {
        String dagJson = WholeSyncDagAssembler.assemble(
                dto.getJobDefinitionInfo(),
                dto.getJobType()
        );
        return buildConfig(dagJson, dto);
    }

    @Override
    public String buildHoconConfigWithStream(BaseSeatunnelJobDefinitionDTO dto) {
        String dagJson = StreamDagAssembler.assemble(
                dto.getJobDefinitionInfo(),
                dto.getJobType()
        );
        return buildConfig(dagJson, dto);
    }

    @Override
    public SeatunnelJobInstanceVO selectById(Long id) {
        SeatunnelJobInstancePO po = getById(id);
        if (po == null) {
            throw new RuntimeException("Job instance not found");
        }
        return ConvertUtil.sourceToTarget(po, SeatunnelJobInstanceVO.class);
    }

    @Override
    public String getLogContent(Long instanceId) {
        SeatunnelJobInstanceVO instance = selectById(instanceId);

        String logPath = instance.getLogPath();
        if (logPath == null || logPath.isEmpty()) {
            return "No log available";
        }

        Path path = Paths.get(logPath);
        if (!Files.exists(path)) {
            return "Log file not found";
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read log file", e);
        }
    }

    @Override
    public boolean existsRunningInstance(Long definitionId) {
        if (definitionId == null) {
            return false;
        }

        long count = lambdaQuery()
                .eq(SeatunnelJobInstancePO::getJobDefinitionId, definitionId)
                .in(SeatunnelJobInstancePO::getJobStatus,
                        JobStatus.INITIALIZING.name(),
                        JobStatus.CREATED.name(),
                        JobStatus.PENDING.name(),
                        JobStatus.SCHEDULED.name(),
                        JobStatus.RUNNING.name(),
                        JobStatus.FAILING.name(),
                        JobStatus.DOING_SAVEPOINT.name(),
                        JobStatus.CANCELING.name())
                .count();

        return count > 0;
    }

    @Override
    public void deleteInstancesByDefinitionId(Long definitionId) {
        if (definitionId == null) {
            return;
        }

        lambdaUpdate()
                .eq(SeatunnelJobInstancePO::getJobDefinitionId, definitionId)
                .remove();
    }

    @Override
    public void deleteMetricsByDefinitionId(Long definitionId) {
        if (definitionId == null) {
            return;
        }

        metricsMapper.delete(
                new LambdaQueryWrapper<SeatunnelJobMetricsPO>()
                        .eq(SeatunnelJobMetricsPO::getJobDefinitionId, definitionId)
        );
    }

    @Override
    public void removeAllByDefinitionId(Long definitionId) {
        if (definitionId == null) {
            return;
        }

        deleteMetricsByDefinitionId(definitionId);

        lambdaUpdate()
                .eq(SeatunnelJobInstancePO::getJobDefinitionId, definitionId)
                .remove();
    }

    @Override
    public void updateStatus(Long instanceId, JobStatus status) {
        updateStatus(instanceId, status, null);
    }

    @Override
    public void updateStatus(Long instanceId, JobStatus status, String errorMessage) {
        if (instanceId == null || status == null) {
            return;
        }

        boolean endState = status.isEndState();

        lambdaUpdate()
                .eq(SeatunnelJobInstancePO::getId, instanceId)
                .set(SeatunnelJobInstancePO::getJobStatus, status.name())
                .set(errorMessage != null && !errorMessage.isBlank(),
                        SeatunnelJobInstancePO::getErrorMessage,
                        truncate(errorMessage, 2000))
                .set(endState, SeatunnelJobInstancePO::getEndTime, new Date())
                .update();
    }

    @Override
    public void updateStatusAndEngineId(Long instanceId, JobStatus status, String engineJobId) {
        if (instanceId == null || status == null) {
            return;
        }

        boolean endState = status.isEndState();

        lambdaUpdate()
                .eq(SeatunnelJobInstancePO::getId, instanceId)
                .set(SeatunnelJobInstancePO::getJobStatus, status.name())
                .set(engineJobId != null && !engineJobId.isBlank(),
                        SeatunnelJobInstancePO::getJobEngineId,
                        engineJobId)
                .set(endState, SeatunnelJobInstancePO::getEndTime, new Date())
                .update();
    }

    @Override
    public void reconcileInstanceStatus(Long instanceId) {
        if (instanceId == null) {
            return;
        }

        SeatunnelJobInstancePO instance = getById(instanceId);
        if (instance == null) {
            return;
        }

        reconcileSingleInstance(instance);
    }

    @Override
    public void reconcileUnfinishedInstanceStatuses() {
        List<SeatunnelJobInstancePO> instances = lambdaQuery()
                .in(SeatunnelJobInstancePO::getJobStatus,
                        JobStatus.INITIALIZING.name(),
                        JobStatus.CREATED.name(),
                        JobStatus.PENDING.name(),
                        JobStatus.SCHEDULED.name(),
                        JobStatus.RUNNING.name(),
                        JobStatus.FAILING.name(),
                        JobStatus.DOING_SAVEPOINT.name(),
                        JobStatus.CANCELING.name())
                .list();

        if (instances == null || instances.isEmpty()) {
            return;
        }

        for (SeatunnelJobInstancePO instance : instances) {
            try {
                reconcileSingleInstance(instance);
            } catch (Exception e) {
                log.error("Reconcile instance status failed, instanceId={}", instance.getId(), e);
            }
        }
    }

    private void reconcileSingleInstance(SeatunnelJobInstancePO instance) {
        if (instance == null) {
            return;
        }

        if (instance.getJobStatus() != null) {
            try {
                JobStatus current = JobStatus.fromString(instance.getJobStatus());
                if (current.isEndState()) {
                    return;
                }
            } catch (Exception ignored) {
                // ignore invalid local state and continue reconcile
            }
        }

        Long engineJobId = instance.getJobEngineId();

        JobStatus actualStatus = queryActualJobStatus(engineJobId);
        updateStatus(instance.getId(), actualStatus);
    }

    private JobStatus queryActualJobStatus(Long engineJobId) {
        try {
            Map<?, ?> jobInfo = seatunnelRestClient.jobInfo(engineJobId);
            JobStatus status = parseJobStatus(jobInfo);
            if (status != null) {
                return status;
            }
        } catch (Exception e) {
            log.warn("Query job-info failed, engineJobId={}", engineJobId, e);
        }

        JobStatus status = queryFromFinishedJobs(engineJobId);
        if (status != null) {
            return status;
        }

        return JobStatus.UNKNOWABLE;
    }

    private JobStatus queryFromFinishedJobs(Long engineJobId) {
        for (JobStatus candidate : new JobStatus[]{
                JobStatus.FINISHED,
                JobStatus.FAILED,
                JobStatus.CANCELED,
                JobStatus.UNKNOWABLE
        }) {
            try {
                List<?> jobs = seatunnelRestClient.finishedJobs(candidate.name());
                if (containsEngineJobId(jobs, engineJobId)) {
                    return candidate;
                }
            } catch (Exception e) {
                log.warn("Query finished-jobs failed, state={}, engineJobId={}",
                        candidate.name(), engineJobId, e);
            }
        }
        return null;
    }

    private boolean containsEngineJobId(List<?> jobs, Long engineJobId) {
        if (jobs == null || jobs.isEmpty()) {
            return false;
        }

        for (Object job : jobs) {
            if (!(job instanceof Map)) {
                continue;
            }

            Map<?, ?> map = (Map<?, ?>) job;

            Object id = firstNonNull(
                    map.get("jobId"),
                    map.get("job_id"),
                    map.get("id"),
                    map.get("jobEngineId")
            );

            if (id != null && Objects.equals(String.valueOf(id), engineJobId)) {
                return true;
            }
        }
        return false;
    }

    private SeatunnelJobInstancePO buildJobInstance(
            SeatunnelBatchJobDefinitionVO definitionVO,
            RunMode runMode) {

        Long id = generateInstanceId();

        String jobConfig = buildHoconConfig(
                ConvertUtil.sourceToTarget(
                        definitionVO,
                        SeatunnelBatchJobDefinitionDTO.class
                )
        );

        return SeatunnelJobInstancePO.builder()
                .id(id)
                .jobDefinitionId(definitionVO.getId())
                .startTime(new Date())
                .jobConfig(jobConfig)
                .logPath(buildLogPath(id))
                .jobStatus(JobStatus.RUNNING.name())
                .runMode(runMode)
                .jobType(JobMode.BATCH)
                .build();
    }

    private Long generateInstanceId() {
        try {
            return CodeGenerateUtils.getInstance().genCode();
        } catch (CodeGenerateUtils.CodeGenerateException e) {
            throw new RuntimeException("Failed to generate job instance ID", e);
        }
    }

    private String buildLogPath(Long id) {
        return System.getProperty("user.dir")
                + File.separator
                + Paths.get(baseLogDir, "job-" + id + ".log");
    }

    private String buildConfig(String dagJson, BaseSeatunnelJobDefinitionDTO dto) {
        DagGraph dagGraph = DagUtil.parseAndCheck(dagJson);
        return configBuilder.build(dagGraph, dto);
    }

    private String parseEngineJobId(Map<?, ?> result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        Object value = firstNonNull(
                result.get("jobId"),
                result.get("job_id"),
                result.get("id"),
                result.get("jobEngineId")
        );

        return value == null ? null : String.valueOf(value);
    }

    private JobStatus parseJobStatus(Map<?, ?> result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        Object value = firstNonNull(
                result.get("jobStatus"),
                result.get("status"),
                result.get("state")
        );

        if (value == null) {
            return null;
        }

        try {
            return JobStatus.fromString(String.valueOf(value));
        } catch (Exception e) {
            log.warn("Unknown job status value: {}", value);
            return null;
        }
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}