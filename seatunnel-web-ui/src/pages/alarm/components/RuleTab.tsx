import { useIntl } from '@umijs/max';
import { Button, Input, message, Modal, Space, Spin, Switch, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { SEVERITY_CONFIG } from '../constants';
import { deleteRule, fetchAllJobDefinitions, fetchAllRuleChannels, fetchChannels, fetchRuleChannels, fetchRules, saveRule } from '../service';
import type { AlarmChannelRecord, AlarmModalRef, AlarmOperateType, AlarmRuleRecord } from '../types';
import AddOrEditRuleModal from './AddOrEditRuleModal';

const { confirm } = Modal;

const RuleTab: React.FC = () => {
  const intl = useIntl();
  const modalRef = useRef<AlarmModalRef>(null);

  const [loading, setLoading] = useState(false);
  const [ruleList, setRuleList] = useState<AlarmRuleRecord[]>([]);
  const [channelList, setChannelList] = useState<AlarmChannelRecord[]>([]);
  const [jobNameMap, setJobNameMap] = useState<Record<number, string>>({});
  const [ruleChannelMap, setRuleChannelMap] = useState<Record<number, number[]>>({});
  const [searchKeyword, setSearchKeyword] = useState('');

  const channelMap = useMemo(() => {
    const map: Record<number, AlarmChannelRecord> = {};
    channelList.forEach((c) => {
      if (c.id != null) map[c.id] = c;
    });
    return map;
  }, [channelList]);

  const fetchList = async () => {
    setLoading(true);
    try {
      const res = await fetchRules();
      if (res?.code === 0) {
        setRuleList(res.data || []);
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchSupportData = async () => {
    const [channelsRes, jobs, ruleChannelsRes] = await Promise.all([
      fetchChannels().catch(() => null),
      fetchAllJobDefinitions().catch(() => [] as any[]),
      fetchAllRuleChannels().catch(() => null),
    ]);
    if (channelsRes?.code === 0) {
      setChannelList(channelsRes.data || []);
    }
    const map: Record<number, string> = {};
    jobs.forEach((j) => {
      map[j.id] = j.jobName;
    });
    setJobNameMap(map);

    // 构建规则 -> 通道 id 列表的映射
    if (ruleChannelsRes?.code === 0 && Array.isArray(ruleChannelsRes.data)) {
      const rcMap: Record<number, number[]> = {};
      ruleChannelsRes.data.forEach((link) => {
        if (!rcMap[link.ruleId]) rcMap[link.ruleId] = [];
        rcMap[link.ruleId].push(link.channelId);
      });
      setRuleChannelMap(rcMap);
    }
  };

  useEffect(() => {
    fetchList();
    fetchSupportData();
  }, []);

  const filteredList = useMemo(() => {
    if (!searchKeyword.trim()) return ruleList;
    const kw = searchKeyword.trim().toLowerCase();
    return ruleList.filter((r) => (r.name || '').toLowerCase().includes(kw));
  }, [ruleList, searchKeyword]);

  const handleRefresh = () => {
    fetchList();
    // 刷新规则-通道关联映射，确保新增/编辑/删除后列表展示同步
    fetchAllRuleChannels()
      .then((res) => {
        if (res?.code === 0 && Array.isArray(res.data)) {
          const rcMap: Record<number, number[]> = {};
          res.data.forEach((link) => {
            if (!rcMap[link.ruleId]) rcMap[link.ruleId] = [];
            rcMap[link.ruleId].push(link.channelId);
          });
          setRuleChannelMap(rcMap);
        }
      })
      .catch(() => {});
  };

  const handleCreate = () => {
    modalRef.current?.open({
      operateType: 'CREATE' as AlarmOperateType,
      onSuccess: handleRefresh,
    });
  };

  const handleEdit = (record: AlarmRuleRecord) => {
    modalRef.current?.open({
      operateType: 'EDIT' as AlarmOperateType,
      currentRecord: record,
      onSuccess: handleRefresh,
    });
  };

  const handleDelete = (record: AlarmRuleRecord) => {
    confirm({
      title: intl.formatMessage({ id: 'pages.alarm.delete.confirmTitle', defaultMessage: '确认删除？' }),
      centered: true,
      content: (
        <span>
          {intl.formatMessage(
            {
              id: 'pages.alarm.rule.delete.confirmContent',
              defaultMessage: '确认删除告警规则 [{name}] 吗？',
            },
            { name: <span style={{ color: 'orange' }}>{record.name}</span> },
          )}
        </span>
      ),
      okText: intl.formatMessage({ id: 'pages.alarm.delete.okText', defaultMessage: '删除' }),
      okType: 'primary',
      okButtonProps: { size: 'small', danger: true },
      cancelButtonProps: { size: 'small' },
      maskClosable: true,
      async onOk() {
        if (!record.id) {
          message.error('id 不存在');
          return;
        }
        try {
          const res = await deleteRule(record.id);
          if (res?.code === 0) {
            message.success(
              intl.formatMessage({
                id: 'pages.alarm.message.deleteSuccess',
                defaultMessage: '删除成功',
              }),
            );
            handleRefresh();
          }
        } catch {
          /* errorHandler 已提示 */
        }
      },
    });
  };

  const handleToggleEnabled = async (record: AlarmRuleRecord, checked: boolean) => {
    if (!record.id) return;
    try {
      // 保留原有通道关联：更新前先查询，避免 relinkChannels 清空
      let channelIds: number[] = [];
      try {
        const linkRes = await fetchRuleChannels(record.id);
        if (linkRes?.code === 0 && Array.isArray(linkRes.data)) {
          channelIds = linkRes.data;
        }
      } catch {
        /* ignore */
      }
      const res = await saveRule({
        id: record.id,
        name: record.name,
        targetJobs: record.targetJobs,
        triggerStatuses: record.triggerStatuses,
        excludes: record.excludes,
        severity: record.severity,
        enabled: checked ? 1 : 0,
        description: record.description,
        channelIds,
      });
      if (res?.code === 0) {
        message.success(
          intl.formatMessage({
            id: checked ? 'pages.alarm.message.enabled' : 'pages.alarm.message.disabled',
            defaultMessage: checked ? '已启用' : '已禁用',
          }),
        );
        handleRefresh();
      }
    } catch {
      /* ignore */
    }
  };

  const columns: ColumnsType<AlarmRuleRecord> = [
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.name', defaultMessage: '名称' }),
      dataIndex: 'name',
      key: 'name',
      ellipsis: true,
      width: 180,
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.targetJob', defaultMessage: '目标任务' }),
      dataIndex: 'targetJobs',
      key: 'targetJobs',
      width: 200,
      render: (targetJobs: string) => {
        if (!targetJobs) {
          return <Tag color="purple">{intl.formatMessage({ id: 'pages.alarm.field.allJobs', defaultMessage: '全部任务' })}</Tag>;
        }
        const ids = targetJobs.split(',').map((s) => s.trim()).filter(Boolean).map(Number);
        return (
          <Space size={[0, 4]} wrap>
            {ids.map((id) => (
              <Tag key={id} bordered={false}>
                {jobNameMap[id] || `#${id}`}
              </Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.triggerStatuses', defaultMessage: '触发状态' }),
      dataIndex: 'triggerStatuses',
      key: 'triggerStatuses',
      render: (statuses: string) =>
        statuses ? (
          <Space size={[0, 4]} wrap>
            {statuses
              .split(',')
              .filter(Boolean)
              .map((s) => (
                <Tag key={s} bordered={false}>
                  {s.trim()}
                </Tag>
              ))}
          </Space>
        ) : (
          '-'
        ),
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.linkedChannels', defaultMessage: '关联通道' }),
      key: 'linkedChannels',
      width: 200,
      render: (_: unknown, record: AlarmRuleRecord) => {
        const channelIds = (record.id != null && ruleChannelMap[record.id]) || [];
        if (channelIds.length === 0) return '-';
        return (
          <Space size={[0, 4]} wrap>
            {channelIds.map((cid) => {
              const ch = channelMap[cid];
              return (
                <Tag key={cid} bordered={false} color={ch?.enabled === 0 ? 'default' : 'blue'}>
                  {ch?.name || `#${cid}`}
                </Tag>
              );
            })}
          </Space>
        );
      },
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.severity', defaultMessage: '严重级别' }),
      dataIndex: 'severity',
      key: 'severity',
      width: 110,
      render: (sev: string) => {
        const cfg = SEVERITY_CONFIG[sev];
        return cfg ? <Tag color={cfg.tagColor}>{cfg.text}</Tag> : <span>{sev || '-'}</span>;
      },
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.enabled', defaultMessage: '状态' }),
      dataIndex: 'enabled',
      key: 'enabled',
      width: 90,
      render: (enabled: number, record) => (
        <Switch
          checked={enabled === 1}
          size="small"
          onChange={(checked) => handleToggleEnabled(record, checked)}
        />
      ),
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.action', defaultMessage: '操作' }),
      key: 'action',
      width: 140,
      render: (_: unknown, record: AlarmRuleRecord) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            {intl.formatMessage({ id: 'pages.alarm.button.edit', defaultMessage: '编辑' })}
          </Button>
          <Button type="link" size="small" danger onClick={() => handleDelete(record)}>
            {intl.formatMessage({ id: 'pages.alarm.button.delete', defaultMessage: '删除' })}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between gap-3">
        <Input.Search
          allowClear
          placeholder={intl.formatMessage({
            id: 'pages.alarm.placeholder.searchRule',
            defaultMessage: '搜索规则名称',
          })}
          style={{ maxWidth: 320 }}
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
        />
        <Button type="primary" onClick={handleCreate}>
          {intl.formatMessage({ id: 'pages.alarm.button.addRule', defaultMessage: '新建规则' })}
        </Button>
      </div>

      <Spin spinning={loading}>
        <Table
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={filteredList}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (t) =>
              intl.formatMessage(
                { id: 'pages.alarm.pagination.total', defaultMessage: '共 {total} 条' },
                { total: t },
              ),
          }}
        />
      </Spin>

      <AddOrEditRuleModal ref={modalRef} channels={channelList} />
    </div>
  );
};

export default RuleTab;
