import { useIntl } from '@umijs/max';
import { Button, Input, message, Modal, Space, Spin, Switch, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { deleteChannel, fetchChannels, saveChannel } from '../service';
import type { AlarmChannelRecord, AlarmModalRef, AlarmOperateType } from '../types';
import { formatTime } from '../utils';
import AddOrEditChannelModal from './AddOrEditChannelModal';

const { confirm } = Modal;

const ChannelTab: React.FC = () => {
  const intl = useIntl();
  const modalRef = useRef<AlarmModalRef>(null);

  const [loading, setLoading] = useState(false);
  const [channelList, setChannelList] = useState<AlarmChannelRecord[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');

  const fetchList = async () => {
    setLoading(true);
    try {
      const res = await fetchChannels();
      if (res?.code === 0) {
        setChannelList(res.data || []);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList();
  }, []);

  const filteredList = useMemo(() => {
    if (!searchKeyword.trim()) return channelList;
    const kw = searchKeyword.trim().toLowerCase();
    return channelList.filter(
      (c) =>
        (c.name || '').toLowerCase().includes(kw) ||
        (c.channelType || '').toLowerCase().includes(kw),
    );
  }, [channelList, searchKeyword]);

  const handleRefresh = () => fetchList();

  const handleCreate = () => {
    modalRef.current?.open({
      operateType: 'CREATE' as AlarmOperateType,
      onSuccess: handleRefresh,
    });
  };

  const handleEdit = (record: AlarmChannelRecord) => {
    modalRef.current?.open({
      operateType: 'EDIT' as AlarmOperateType,
      currentRecord: record,
      onSuccess: handleRefresh,
    });
  };

  const handleDelete = (record: AlarmChannelRecord) => {
    confirm({
      title: intl.formatMessage({
        id: 'pages.alarm.delete.confirmTitle',
        defaultMessage: '确认删除？',
      }),
      centered: true,
      content: (
        <span>
          {intl.formatMessage(
            {
              id: 'pages.alarm.channel.delete.confirmContent',
              defaultMessage: '确认删除告警通道 [{name}] 吗？',
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
          const res = await deleteChannel(record.id);
          if (res?.code === 0) {
            message.success(res.msg || '删除成功');
            handleRefresh();
          }
        } catch {
          /* errorHandler 已提示 */
        }
      },
    });
  };

  /** 启停切换 */
  const handleToggleEnabled = async (record: AlarmChannelRecord, checked: boolean) => {
    if (!record.id) return;
    try {
      const res = await saveChannel({
        id: record.id,
        name: record.name,
        channelType: record.channelType,
        configJson: record.configJson,
        enabled: checked ? 1 : 0,
        description: record.description,
      });
      if (res?.code === 0) {
        message.success(checked ? '已启用' : '已禁用');
        handleRefresh();
      }
    } catch {
      /* ignore */
    }
  };

  const columns: ColumnsType<AlarmChannelRecord> = [
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.name', defaultMessage: '名称' }),
      dataIndex: 'name',
      key: 'name',
      ellipsis: true,
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.type', defaultMessage: '类型' }),
      dataIndex: 'channelType',
      key: 'channelType',
      width: 140,
      render: (type: string) =>
        type ? <Tag color="blue">{type}</Tag> : '-',
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.enabled', defaultMessage: '状态' }),
      dataIndex: 'enabled',
      key: 'enabled',
      width: 100,
      render: (enabled: number, record) => (
        <Switch
          checked={enabled === 1}
          size="small"
          onChange={(checked) => handleToggleEnabled(record, checked)}
        />
      ),
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.description', defaultMessage: '描述' }),
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (text: string) => text || '-',
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.createTime', defaultMessage: '创建时间' }),
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (t: string) => (t ? formatTime(t) : '-'),
    },
    {
      title: intl.formatMessage({ id: 'pages.alarm.table.col.action', defaultMessage: '操作' }),
      key: 'action',
      width: 140,
      render: (_: unknown, record: AlarmChannelRecord) => (
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
            id: 'pages.alarm.placeholder.searchChannel',
            defaultMessage: '搜索通道名称或类型',
          })}
          style={{ maxWidth: 320 }}
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
        />
        <Button type="primary" onClick={handleCreate}>
          {intl.formatMessage({ id: 'pages.alarm.button.addChannel', defaultMessage: '新建通道' })}
        </Button>
      </div>

      <Spin spinning={loading}>
        <Table
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={filteredList}
          pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
        />
      </Spin>

      <AddOrEditChannelModal ref={modalRef} />
    </div>
  );
};

export default ChannelTab;
