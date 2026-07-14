package org.apache.seatunnel.web.api.alarm.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.web.api.alarm.repository.AlarmRuleTargetRepository;
import org.apache.seatunnel.web.api.alarm.repository.AlarmTarget;
import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRecordEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmChannelMapper;
import org.apache.seatunnel.web.dao.mapper.AlarmRecordMapper;
import org.apache.seatunnel.web.dao.mapper.AlarmRuleChannelMapper;
import org.apache.seatunnel.web.dao.mapper.AlarmRuleMapper;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MyBatis-Plus backed implementation of {@link AlarmRuleTargetRepository}.
 */
@Repository
@Slf4j
public class AlarmRuleTargetRepositoryImpl implements AlarmRuleTargetRepository {

    @Resource
    private AlarmRuleMapper ruleMapper;

    @Resource
    private AlarmRuleChannelMapper ruleChannelMapper;

    @Resource
    private AlarmChannelMapper channelMapper;

    @Resource
    private AlarmRecordMapper recordMapper;

    @Override
    public List<AlarmTarget> findMatchedTargets(Long jobDefinitionId, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<AlarmRuleEntity> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(AlarmRuleEntity::getEnabled, 1)
                .and(q -> q.eq(jobDefinitionId != null, AlarmRuleEntity::getJobDefinitionId, jobDefinitionId)
                        .or()
                        .isNull(AlarmRuleEntity::getJobDefinitionId));

        List<AlarmRuleEntity> rules = ruleMapper.selectList(ruleWrapper);
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        // Precise status matching in Java to avoid SQL LIKE false positives.
        List<AlarmRuleEntity> matched = rules.stream()
                .filter(r -> statusMatches(r.getTriggerStatuses(), newStatus))
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ruleIds = matched.stream().map(AlarmRuleEntity::getId).collect(Collectors.toList());

        LambdaQueryWrapper<AlarmRuleChannelEntity> linkWrapper = new LambdaQueryWrapper<>();
        linkWrapper.in(AlarmRuleChannelEntity::getRuleId, ruleIds);
        List<AlarmRuleChannelEntity> links = ruleChannelMapper.selectList(linkWrapper);
        if (links == null || links.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<AlarmRuleChannelEntity>> linksByRule = links.stream()
                .collect(Collectors.groupingBy(AlarmRuleChannelEntity::getRuleId));

        Set<Long> channelIds = links.stream()
                .map(AlarmRuleChannelEntity::getChannelId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<AlarmChannelEntity> channelWrapper = new LambdaQueryWrapper<>();
        channelWrapper.in(AlarmChannelEntity::getId, channelIds)
                .eq(AlarmChannelEntity::getEnabled, 1);
        List<AlarmChannelEntity> channels = channelMapper.selectList(channelWrapper);
        Map<Long, AlarmChannelEntity> channelMap = channels.stream()
                .collect(Collectors.toMap(AlarmChannelEntity::getId, c -> c));

        return matched.stream()
                .map(rule -> buildTarget(rule, linksByRule.getOrDefault(rule.getId(), Collections.emptyList()),
                        channelMap))
                .filter(t -> t.getChannels() != null && !t.getChannels().isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public void saveRecord(AlarmRecordEntity record) {
        if (record == null) {
            return;
        }
        recordMapper.insert(record);
    }

    private AlarmTarget buildTarget(AlarmRuleEntity rule,
                                    List<AlarmRuleChannelEntity> links,
                                    Map<Long, AlarmChannelEntity> channelMap) {
        List<AlarmTarget.AlarmTargetChannel> channels = links.stream()
                .map(AlarmRuleChannelEntity::getChannelId)
                .map(channelMap::get)
                .filter(c -> c != null)
                .map(c -> AlarmTarget.AlarmTargetChannel.builder()
                        .channelId(c.getId())
                        .channelType(c.getChannelType())
                        .configJson(c.getConfigJson())
                        .build())
                .collect(Collectors.toList());

        return AlarmTarget.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .severity(rule.getSeverity())
                .channels(channels)
                .build();
    }

    private boolean statusMatches(String triggerStatuses, String newStatus) {
        if (triggerStatuses == null || triggerStatuses.isBlank()) {
            return false;
        }
        return Arrays.stream(triggerStatuses.split(","))
                .map(String::trim)
                .anyMatch(newStatus::equals);
    }
}
