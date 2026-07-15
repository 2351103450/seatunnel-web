package org.apache.seatunnel.web.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.seatunnel.plugin.alarm.api.AlarmChannel;
import org.apache.seatunnel.plugin.alarm.api.AlarmChannelFactory;
import org.apache.seatunnel.plugin.alarm.api.AlarmData;
import org.apache.seatunnel.plugin.alarm.api.AlarmInfo;
import org.apache.seatunnel.plugin.alarm.api.AlarmResult;
import org.apache.seatunnel.plugin.alarm.api.AlarmSeverity;
import org.apache.seatunnel.web.api.alarm.engine.AlarmConfigParser;
import org.apache.seatunnel.web.api.alarm.plugin.AlarmPluginManager;
import org.apache.seatunnel.web.api.service.AlarmChannelService;
import org.apache.seatunnel.web.api.service.AlarmRecordService;
import org.apache.seatunnel.web.api.service.AlarmRuleService;
import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRecordEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleEntity;
import org.apache.seatunnel.web.spi.bean.entity.Result;
import org.apache.seatunnel.web.spi.form.FormFieldConfig;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Tag(name = "ALARM_TAG")
@RequestMapping("/api/v1/alarm")
public class AlarmController {

    @Resource
    private AlarmChannelService alarmChannelService;

    @Resource
    private AlarmRuleService alarmRuleService;

    @Resource
    private AlarmRecordService alarmRecordService;

    @Resource
    private AlarmPluginManager alarmPluginManager;

    @Resource
    private ObjectMapper objectMapper;

    // -------------------- channel types (from SPI) --------------------

    @GetMapping("/channel-types")
    @Operation(summary = "listChannelTypes", description = "List all alarm channel types discovered via SPI")
    public Result<List<ChannelTypeVO>> listChannelTypes() {
        List<ChannelTypeVO> list = alarmPluginManager.getFactoryMap().entrySet().stream()
                .map(e -> {
                    AlarmChannelFactory factory = e.getValue();
                    ChannelTypeVO vo = new ChannelTypeVO();
                    vo.setChannelType(factory.name());
                    vo.setDisplayName(factory.name());
                    vo.setConfigFields(factory.params());
                    return vo;
                })
                .collect(Collectors.toList());
        return Result.buildSuc(list);
    }

    // -------------------- channels --------------------

    @GetMapping("/channels")
    @Operation(summary = "listChannels", description = "List all alarm channel instances")
    public Result<List<AlarmChannelEntity>> listChannels() {
        return Result.buildSuc(alarmChannelService.list());
    }

    @PostMapping("/channels")
    @Operation(summary = "saveChannel", description = "Create or update an alarm channel instance")
    public Result<Long> saveChannel(@RequestBody AlarmChannelEntity entity) {
        if (entity.getId() == null) {
            return Result.buildSuc(alarmChannelService.create(entity));
        }
        return Result.build(alarmChannelService.update(entity), entity.getId());
    }

    @DeleteMapping("/channels/{id}")
    @Operation(summary = "deleteChannel", description = "Delete an alarm channel instance")
    public Result<Boolean> deleteChannel(@PathVariable("id") Long id) {
        return Result.buildSuc(alarmChannelService.delete(id));
    }

    @PostMapping("/channels/test")
    @Operation(summary = "testChannel", description = "Send a test alarm to verify channel connectivity")
    public Result<TestChannelResultVO> testChannel(@RequestBody TestChannelCommand cmd) {
        AlarmChannelFactory factory = alarmPluginManager.getFactoryMap().get(cmd.getChannelType());
        if (factory == null) {
            TestChannelResultVO vo = new TestChannelResultVO();
            vo.setSuccess(false);
            vo.setMessage("未找到通道类型: " + cmd.getChannelType());
            return Result.buildSuc(vo);
        }
        AlarmChannel channel = factory.create();
        Map<String, String> params = AlarmConfigParser.parse(objectMapper, cmd.getConfigJson());
        AlarmData data = AlarmData.builder()
                .id(0L)
                .title("SeaTunnel 告警连通性测试")
                .content("这是一条测试消息，用于验证告警通道配置是否正确。\n如收到此消息，说明通道连通性正常。")
                .severity(AlarmSeverity.INFO)
                .build();
        AlarmInfo info = AlarmInfo.builder()
                .alarmParams(params)
                .alarmData(data)
                .build();
        AlarmResult result = channel.process(info);

        // Persist test record so user can see it in the record list
        AlarmRecordEntity record = new AlarmRecordEntity();
        record.setRuleId(0L);
        record.setChannelId(0L);
        record.setChannelType(cmd.getChannelType());
        record.setJobInstanceId(0L);
        record.setJobDefinitionId(0L);
        record.setJobName("连通性测试");
        record.setNewStatus("TEST");
        record.setSeverity(data.getSeverity() == null ? null : data.getSeverity().name());
        record.setSuccess(result.isSuccess() ? 1 : 0);
        record.setErrorMessage(result.isSuccess() ? null : result.getMessage());
        record.setContent(data.getContent());
        alarmRecordService.save(record);

        TestChannelResultVO vo = new TestChannelResultVO();
        vo.setSuccess(result.isSuccess());
        vo.setMessage(result.getMessage());
        return Result.buildSuc(vo);
    }

    // -------------------- rules --------------------

    @GetMapping("/rules")
    @Operation(summary = "listRules", description = "List all alarm rules")
    public Result<List<AlarmRuleEntity>> listRules() {
        return Result.buildSuc(alarmRuleService.list());
    }

    @PostMapping("/rules")
    @Operation(summary = "saveRule", description = "Create or update an alarm rule with linked channels")
    public Result<Long> saveRule(@RequestBody AlarmRuleService.AlarmRuleCommand command) {
        if (command.getId() == null) {
            return Result.buildSuc(alarmRuleService.create(command));
        }
        return Result.build(alarmRuleService.update(command), command.getId());
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "deleteRule", description = "Delete an alarm rule")
    public Result<Boolean> deleteRule(@PathVariable("id") Long id) {
        return Result.buildSuc(alarmRuleService.delete(id));
    }

    @GetMapping("/rules/{id}/channels")
    @Operation(summary = "listRuleChannels", description = "List channel ids linked to a rule")
    public Result<List<Long>> listRuleChannels(@PathVariable("id") Long id) {
        List<Long> ids = alarmRuleService.listChannels(id).stream()
                .map(link -> link.getChannelId())
                .collect(Collectors.toList());
        return Result.buildSuc(ids);
    }

    @GetMapping("/rules/all-channels")
    @Operation(summary = "listAllRuleChannels", description = "List all rule-channel links for batch loading")
    public Result<List<AlarmRuleChannelEntity>> listAllRuleChannels() {
        return Result.buildSuc(alarmRuleService.listAllChannels());
    }

    // -------------------- records --------------------

    @GetMapping("/records")
    @Operation(summary = "pageRecords", description = "Page alarm delivery records with optional filters")
    public Result<RecordPageVO> pageRecords(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "jobInstanceId", required = false) Long jobInstanceId,
            @RequestParam(value = "channelType", required = false) String channelType,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "success", required = false) Integer success) {
        IPage<AlarmRecordEntity> page = alarmRecordService.page(
                pageNo, pageSize, jobInstanceId, channelType, severity, success);
        RecordPageVO vo = new RecordPageVO();
        vo.setList(page.getRecords());
        vo.setTotal(page.getTotal());
        return Result.buildSuc(vo);
    }

    @Data
    public static class ChannelTypeVO {
        private String channelType;
        private String displayName;
        private List<FormFieldConfig> configFields = new ArrayList<>();
    }

    @Data
    public static class TestChannelCommand {
        private String channelType;
        private String configJson;
    }

    @Data
    public static class TestChannelResultVO {
        private boolean success;
        private String message;
    }

    @Data
    public static class RecordPageVO {
        private List<AlarmRecordEntity> list;
        private long total;
    }
}
