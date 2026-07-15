import { useIntl } from '@umijs/max';
import { Button, Space, Spin, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import React, { useCallback, useEffect, useState } from 'react';
import { JOB_STATUS_TAG_COLOR, SEVERITY_CONFIG } from '../constants';
import { fetchAlarmRecords } from '../service';
import type { AlarmRecordRecord } from '../types';
import { formatTime } from '../utils';

const { Paragraph } = Typography;

const DEFAULT_PAGE_SIZE = 10;

const RecordTab: React.FC = () => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [records, setRecords] = useState<AlarmRecordRecord[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  // 后端不返回 total，前端据此推断末页以驱动分页器
  const [total, setTotal] = useState(0);

  const fetchRecords = useCallback(async (page: number, size: number) => {
    setLoading(true);
    try {
      const res = await fetchAlarmRecords({ pageNo: page, pageSize: size });
      if (res?.code === 0) {
        const list = res.data || [];
        setRecords(list);
        // 推断总数：当本页不足 size 时为末页，否则预留下一页
        if (list.length < size) {
          setTotal((page - 1) * size + list.length);
        } else {
          setTotal(page * size + 1);
        }
      } else {
        setRecords([]);
      }
    } catch {
      setRecords([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRecords(1, DEFAULT_PAGE_SIZE);
  }, [fetchRecords]);

  const handlePageChange = (pagination: TablePaginationConfig) => {
    const { current = 1, pageSize: nextSize = DEFAULT_PAGE_SIZE } = pagination;
    setPageNo(current);
    setPageSize(nextSize);
    fetchRecords(current, nextSize);
  };

  const handleRefresh = () => {
    fetchRecords(pageNo, pageSize);
  };

  const columns: ColumnsType<AlarmRecordRecord> = [
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.jobName', defaultMessage: '任务名称' }),
      dataIndex: 'jobName',
      key: 'jobName',
      width: 180,
      ellipsis: true,
      render: (v: string) => v || '-',
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.newStatus', defaultMessage: '任务状态' }),
      dataIndex: 'newStatus',
      key: 'newStatus',
      width: 130,
      render: (v: string) =>
        v ? <Tag color={JOB_STATUS_TAG_COLOR[v] || 'default'}>{v}</Tag> : '-',
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
      title: intl.formatMessage({ id: 'pages.alarm.table.col.channelType', defaultMessage: '通道类型' }),
      dataIndex: 'channelType',
      key: 'channelType',
      width: 130,
      render: (v: string) => (v ? <Tag bordered={false}>{v}</Tag> : '-'),
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.success', defaultMessage: '投递结果' }),
      dataIndex: 'success',
      key: 'success',
      width: 110,
      render: (v: number) =>
        v === 1 ? <Tag color="success">成功</Tag> : <Tag color="error">失败</Tag>,
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.errorMessage', defaultMessage: '错误信息' }),
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
      title: intl.formatMessage({ id: 'pages.alarm.table.col.content', defaultMessage: '告警内容' }),
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
      title: intl.formatMessage({ id: 'pages.alarm.table.col.sentTime', defaultMessage: '发送时间' }),
      dataIndex: 'sentTime',
      key: 'sentTime',
      width: 180,
      render: (t: string) => formatTime(t),
    },
  ];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between gap-3">
        <span className="text-sm text-[#667085]">
          {intl.formatMessage({
            id: 'pages.alarm.record.hint',
            defaultMessage: '展示告警投递记录，仅可查看。',
          })}
        </span>
        <Space>
          <Button onClick={handleRefresh}>
            {intl.formatMessage({ id: 'pages.alarm.button.refresh', defaultMessage: '刷新' })}
          </Button>
        </Space>
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
            showTotal: (t) => `共 ${t} 条`,
          }}
          onChange={handlePageChange}
        />
      </Spin>
    </div>
  );
};

export default RecordTab;
