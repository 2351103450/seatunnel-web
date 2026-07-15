import { useIntl } from '@umijs/max';
import { Button, Select, Space, Spin, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { fetchAlarmRecords, fetchChannels } from '../service';
import type { AlarmChannelRecord, AlarmRecordRecord } from '../types';
import { formatTime } from '../utils';

const { Paragraph } = Typography;

const DEFAULT_PAGE_SIZE = 10;

const RecordTab: React.FC = () => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [records, setRecords] = useState<AlarmRecordRecord[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);

  // 筛选条件
  const [filterChannelType, setFilterChannelType] = useState<string | undefined>();
  const [filterSeverity, setFilterSeverity] = useState<string | undefined>();
  const [filterSuccess, setFilterSuccess] = useState<number | undefined>();

  // 通道类型选项（从通道列表中提取去重）
  const [channelTypes, setChannelTypes] = useState<string[]>([]);

  const fetchRecords = useCallback(
    async (
      page: number,
      size: number,
      filters?: { channelType?: string; severity?: string; success?: number },
    ) => {
      setLoading(true);
      try {
        const res = await fetchAlarmRecords({
          pageNo: page,
          pageSize: size,
          channelType: filters?.channelType,
          severity: filters?.severity,
          success: filters?.success,
        });
        if (res?.code === 0 && res.data) {
          setRecords(res.data.list || []);
          setTotal(res.data.total || 0);
        } else {
          setRecords([]);
          setTotal(0);
        }
      } catch {
        setRecords([]);
        setTotal(0);
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    // 加载通道类型选项
    fetchChannels()
      .then((res) => {
        if (res?.code === 0 && res.data) {
          const types = Array.from(
            new Set(res.data.map((c: AlarmChannelRecord) => c.channelType).filter(Boolean)),
          ) as string[];
          setChannelTypes(types);
        }
      })
      .catch(() => {});
    fetchRecords(1, DEFAULT_PAGE_SIZE);
  }, [fetchRecords]);

  const handlePageChange = (pagination: TablePaginationConfig) => {
    const { current = 1, pageSize: nextSize = DEFAULT_PAGE_SIZE } = pagination;
    setPageNo(current);
    setPageSize(nextSize);
    fetchRecords(current, nextSize, {
      channelType: filterChannelType,
      severity: filterSeverity,
      success: filterSuccess,
    });
  };

  const handleFilter = () => {
    setPageNo(1);
    fetchRecords(1, pageSize, {
      channelType: filterChannelType,
      severity: filterSeverity,
      success: filterSuccess,
    });
  };

  const handleResetFilters = () => {
    setFilterChannelType(undefined);
    setFilterSeverity(undefined);
    setFilterSuccess(undefined);
    setPageNo(1);
    fetchRecords(1, pageSize);
  };

  const handleRefresh = () => {
    fetchRecords(pageNo, pageSize, {
      channelType: filterChannelType,
      severity: filterSeverity,
      success: filterSuccess,
    });
  };

  const hasActiveFilters = useMemo(
    () => filterChannelType != null || filterSeverity != null || filterSuccess != null,
    [filterChannelType, filterSeverity, filterSuccess],
  );

  const columns: ColumnsType<AlarmRecordRecord> = [
    {
      title: intl.formatMessage({
        id: 'pages.alarm.table.col.jobName',
        defaultMessage: '任务名称',
      }),
      dataIndex: 'jobName',
      key: 'jobName',
      width: 180,
      ellipsis: true,
      render: (v: string) => v || '-',
    },
    {
      title: intl.formatMessage({
        id: 'pages.alarm.table.col.newStatus',
        defaultMessage: '任务状态',
      }),
      dataIndex: 'newStatus',
      key: 'newStatus',
      width: 130,
      render: (v: string) => (v ? <Tag color={getStatusColor(v)}>{v}</Tag> : '-'),
    },
    {
      title: intl.formatMessage({
        id: 'pages.alarm.table.col.severity',
        defaultMessage: '严重级别',
      }),
      dataIndex: 'severity',
      key: 'severity',
      width: 110,
      render: (sev: string) => getSeverityTag(sev),
    },
    {
      title: intl.formatMessage({
        id: 'pages.alarm.table.col.channelType',
        defaultMessage: '通道类型',
      }),
      dataIndex: 'channelType',
      key: 'channelType',
      width: 130,
      render: (v: string) => (v ? <Tag bordered={false}>{v}</Tag> : '-'),
    },
    {
      title: intl.formatMessage({
        id: 'pages.alarm.table.col.success',
        defaultMessage: '投递结果',
      }),
      dataIndex: 'success',
      key: 'success',
      width: 110,
      render: (v: number) =>
        v === 1 ? (
          <Tag color="success">
            {intl.formatMessage({ id: 'pages.alarm.record.success', defaultMessage: '成功' })}
          </Tag>
        ) : (
          <Tag color="error">
            {intl.formatMessage({ id: 'pages.alarm.record.fail', defaultMessage: '失败' })}
          </Tag>
        ),
    },
    {
      title: intl.formatMessage({
        id: 'pages.alarm.table.col.errorMessage',
        defaultMessage: '错误信息',
      }),
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      ellipsis: true,
      render: (v: string) =>
        v ? (
          <Tooltip title={v}>
            <span>{v}</span>
          </Tooltip>
        ) : (
          '-'
        ),
    },
    {
      title: intl.formatMessage({
        id: 'pages.alarm.table.col.content',
        defaultMessage: '告警内容',
      }),
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
      render: (v: string) =>
        v ? (
          <Tooltip title={<Paragraph style={{ maxWidth: 420, marginBottom: 0 }}>{v}</Paragraph>}>
            <span>{v}</span>
          </Tooltip>
        ) : (
          '-'
        ),
    },
    {
      title: intl.formatMessage({
        id: 'pages.alarm.table.col.sentTime',
        defaultMessage: '发送时间',
      }),
      dataIndex: 'sentTime',
      key: 'sentTime',
      width: 180,
      render: (t: string) => formatTime(t),
    },
  ];

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Select
          allowClear
          placeholder={intl.formatMessage({
            id: 'pages.alarm.placeholder.filterChannelType',
            defaultMessage: '通道类型',
          })}
          style={{ width: 160 }}
          value={filterChannelType}
          onChange={(v) => setFilterChannelType(v)}
          options={channelTypes.map((t) => ({ label: t, value: t }))}
        />
        <Select
          allowClear
          placeholder={intl.formatMessage({
            id: 'pages.alarm.placeholder.filterSeverity',
            defaultMessage: '严重级别',
          })}
          style={{ width: 140 }}
          value={filterSeverity}
          onChange={(v) => setFilterSeverity(v)}
          options={[
            { label: 'INFO', value: 'INFO' },
            { label: 'WARN', value: 'WARN' },
            { label: 'CRITICAL', value: 'CRITICAL' },
          ]}
        />
        <Select
          allowClear
          placeholder={intl.formatMessage({
            id: 'pages.alarm.placeholder.filterResult',
            defaultMessage: '投递结果',
          })}
          style={{ width: 140 }}
          value={filterSuccess}
          onChange={(v) => setFilterSuccess(v)}
          options={[
            {
              label: intl.formatMessage({ id: 'pages.alarm.record.success', defaultMessage: '成功' }),
              value: 1,
            },
            {
              label: intl.formatMessage({ id: 'pages.alarm.record.fail', defaultMessage: '失败' }),
              value: 0,
            },
          ]}
        />
        <Space>
          <Button type="primary" onClick={handleFilter}>
            {intl.formatMessage({ id: 'pages.alarm.button.filter', defaultMessage: '筛选' })}
          </Button>
          {hasActiveFilters && (
            <Button onClick={handleResetFilters}>
              {intl.formatMessage({ id: 'pages.alarm.button.reset', defaultMessage: '重置' })}
            </Button>
          )}
        </Space>
        <div className="flex-1" />
        <Button onClick={handleRefresh}>
          {intl.formatMessage({ id: 'pages.alarm.button.refresh', defaultMessage: '刷新' })}
        </Button>
      </div>

      <Spin spinning={loading}>
        <Table
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={records}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) =>
              intl.formatMessage(
                { id: 'pages.alarm.pagination.total', defaultMessage: '共 {total} 条' },
                { total: t },
              ),
          }}
          onChange={handlePageChange}
        />
      </Spin>
    </div>
  );
};

/** 状态颜色映射 */
function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    RUNNING: 'processing',
    SCHEDULED: 'processing',
    FAILED: 'error',
    FAILING: 'error',
    FINISHED: 'success',
    CANCELED: 'default',
    CANCELING: 'warning',
  };
  return map[status] || 'default';
}

/** 严重级别 Tag */
function getSeverityTag(sev: string): React.ReactNode {
  const map: Record<string, { text: string; color: string }> = {
    INFO: { text: '信息', color: 'blue' },
    WARN: { text: '警告', color: 'orange' },
    CRITICAL: { text: '严重', color: 'red' },
  };
  const cfg = map[sev];
  return cfg ? <Tag color={cfg.color}>{cfg.text}</Tag> : <span>{sev || '-'}</span>;
}

export default RecordTab;
