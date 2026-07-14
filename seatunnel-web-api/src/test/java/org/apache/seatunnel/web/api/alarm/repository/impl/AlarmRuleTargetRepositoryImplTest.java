package org.apache.seatunnel.web.api.alarm.repository.impl;

import org.apache.seatunnel.web.api.alarm.repository.AlarmTarget;
import org.apache.seatunnel.web.dao.entity.AlarmChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleChannelEntity;
import org.apache.seatunnel.web.dao.entity.AlarmRuleEntity;
import org.apache.seatunnel.web.dao.mapper.AlarmChannelMapper;
import org.apache.seatunnel.web.dao.mapper.AlarmRecordMapper;
import org.apache.seatunnel.web.dao.mapper.AlarmRuleChannelMapper;
import org.apache.seatunnel.web.dao.mapper.AlarmRuleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies the rule-matching predicate of {@link AlarmRuleTargetRepositoryImpl},
 * in particular the {@code excludes} black-list, without a database.
 */
@ExtendWith(MockitoExtension.class)
class AlarmRuleTargetRepositoryImplTest {

    @Mock
    private AlarmRuleMapper ruleMapper;

    @Mock
    private AlarmRuleChannelMapper ruleChannelMapper;

    @Mock
    private AlarmChannelMapper channelMapper;

    @Mock
    private AlarmRecordMapper recordMapper;

    @InjectMocks
    private AlarmRuleTargetRepositoryImpl repository;

    @Test
    void ruleExcludingThisTaskShouldBeSkipped() {
        // Rule matches FAILED but explicitly excludes job definition 200.
        AlarmRuleEntity rule = AlarmRuleEntity.builder()
                .name("all-but-noisy")
                .jobDefinitionId(null)
                .triggerStatuses("FAILED")
                .excludes("200")
                .severity("CRITICAL")
                .enabled(1)
                .build();
        rule.setId(1L);
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule));

        List<AlarmTarget> targets = repository.findMatchedTargets(200L, "FAILED");

        assertTrue(targets.isEmpty(), "rule that excludes this task must not fire");
    }

    @Test
    void ruleExcludingOtherTaskShouldStillFire() {
        AlarmRuleEntity rule = AlarmRuleEntity.builder()
                .name("all")
                .jobDefinitionId(null)
                .triggerStatuses("FAILED")
                .excludes("999")
                .severity("CRITICAL")
                .enabled(1)
                .build();
        rule.setId(1L);
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule));
        when(ruleChannelMapper.selectList(any())).thenReturn(List.of(
                AlarmRuleChannelEntity.builder().ruleId(1L).channelId(10L).build()));
        AlarmChannelEntity channel = AlarmChannelEntity.builder()
                .channelType("WEBHOOK")
                .enabled(1)
                .build();
        channel.setId(10L);
        when(channelMapper.selectList(any())).thenReturn(List.of(channel));

        List<AlarmTarget> targets = repository.findMatchedTargets(200L, "FAILED");

        assertEquals(1, targets.size());
        assertEquals("WEBHOOK", targets.get(0).getChannels().get(0).getChannelType());
    }

    @Test
    void disabledStatusShouldNotMatch() {
        AlarmRuleEntity rule = AlarmRuleEntity.builder()
                .triggerStatuses("FAILED")
                .enabled(1)
                .build();
        rule.setId(1L);
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule));

        List<AlarmTarget> targets = repository.findMatchedTargets(200L, "FINISHED");

        assertTrue(targets.isEmpty(), "a FINISHED status must not match a FAILED-only rule");
    }
}
