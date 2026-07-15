import { useIntl } from '@umijs/max';
import { Button, Form, Input, message, Modal, Select, Spin, Switch } from 'antd';
import React, { forwardRef, useImperativeHandle, useRef, useState } from 'react';
import { DEFAULT_SEVERITY, JOB_STATUS_OPTIONS, SEVERITY_OPTIONS } from '../constants';
import { fetchAllJobDefinitions, fetchRuleChannels, saveRule } from '../service';
import type {
  AlarmModalOpenPayload,
  AlarmModalRef,
  AlarmOperateType,
  AlarmRuleCommand,
  AlarmRuleRecord,
  JobDefinitionOption,
  RuleFormValues,
} from '../types';

const { TextArea } = Input;

interface AddOrEditRuleModalProps {
  /** 可选通道列表，用于关联通道多选 */
  channels: Array<{ id?: number; name?: string; channelType?: string; enabled?: number }>;
}

const AddOrEditRuleModal = forwardRef<AlarmModalRef, AddOrEditRuleModalProps>(
  ({ channels }, ref) => {
    const intl = useIntl();
    const [form] = Form.useForm<RuleFormValues>();

    const [open, setOpen] = useState(false);
    const [confirmLoading, setConfirmLoading] = useState(false);
    const [loadingJobs, setLoadingJobs] = useState(false);
    const [operateType, setOperateType] = useState<AlarmOperateType>(
      'CREATE' as AlarmOperateType,
    );
    const [currentRecord, setCurrentRecord] = useState<AlarmRuleRecord>();
    const [jobOptions, setJobOptions] = useState<JobDefinitionOption[]>([]);

    const successCallbackRef = useRef<(() => void) | undefined>();
    const isCreateMode = operateType === ('CREATE' as AlarmOperateType);

    const resetState = () => {
      setCurrentRecord(undefined);
      form.resetFields();
    };

    const handleClose = () => {
      setOpen(false);
      resetState();
    };

    const ensureJobOptions = async () => {
      if (jobOptions.length > 0) return jobOptions;
      setLoadingJobs(true);
      try {
        const list = await fetchAllJobDefinitions();
        setJobOptions(list);
        return list;
      } finally {
        setLoadingJobs(false);
      }
    };

    useImperativeHandle(ref, () => ({
      open: async ({
        operateType: nextOperateType,
        currentRecord: nextRecord,
        onSuccess,
      }: AlarmModalOpenPayload) => {
        resetState();
        setOpen(true);
        setOperateType(nextOperateType);
        setCurrentRecord(nextRecord as AlarmRuleRecord);
        successCallbackRef.current = onSuccess;

        // 拉取任务定义选项
        ensureJobOptions();

        // 默认值
        const defaults: Partial<RuleFormValues> = {
          severity: DEFAULT_SEVERITY,
          enabled: true,
          triggerStatuses: ['FAILED'],
          channelIds: [],
        };

        if (nextOperateType === ('EDIT' as AlarmOperateType) && nextRecord) {
          const rule = nextRecord as AlarmRuleRecord;
          const triggerStatuses = (rule.triggerStatuses || '')
            .split(',')
            .map((s) => s.trim())
            .filter(Boolean);

          form.setFieldsValue({
            name: rule.name || '',
            targetJobs: (rule.targetJobs || '').split(',').map((s) => s.trim()).filter(Boolean).map(Number),
            triggerStatuses,
            severity: rule.severity || DEFAULT_SEVERITY,
            enabled: rule.enabled !== 0,
            description: rule.description || '',
            excludes: (rule.excludes || '').split(',').map((s) => s.trim()).filter(Boolean).map(Number),
            channelIds: [],
          });

          // 回填关联通道 id
          if (rule.id) {
            try {
              const res = await fetchRuleChannels(rule.id);
              if (res?.code === 0 && Array.isArray(res.data)) {
                form.setFieldValue('channelIds', res.data);
              }
            } catch {
              /* ignore */
            }
          }
        } else {
          form.setFieldsValue(defaults);
        }
      },
      close: handleClose,
    }));

    const handleSubmit = async () => {
      try {
        const values = await form.validateFields();

        const targetSet = new Set(values.targetJobs || []);
        // 确保 excludes 是 targetJobs 的子集
        const filteredExcludes = (values.excludes || []).filter((id: number) =>
          targetSet.size === 0 || targetSet.has(id),
        );

        const payload: AlarmRuleCommand = {
          name: values.name,
          targetJobs: (values.targetJobs || []).length > 0 ? (values.targetJobs || []).join(',') : undefined,
          triggerStatuses: (values.triggerStatuses || []).join(','),
          excludes: filteredExcludes.length > 0 ? filteredExcludes.join(',') : undefined,
          severity: values.severity,
          enabled: values.enabled ? 1 : 0,
          description: values.description,
          channelIds: values.channelIds || [],
        };

        if (!isCreateMode && currentRecord?.id) {
          payload.id = currentRecord.id;
        }

        setConfirmLoading(true);
        const res = await saveRule(payload);

        if (res?.code !== 0) return;

        message.success(
          intl.formatMessage({
            id: 'pages.alarm.message.success',
            defaultMessage: '操作成功',
          }),
        );
        handleClose();
        successCallbackRef.current?.();
      } catch (error: any) {
        if (error?.errorFields) return;
      } finally {
        setConfirmLoading(false);
      }
    };

    const modalTitle = isCreateMode
      ? intl.formatMessage({ id: 'pages.alarm.modal.rule.title.add', defaultMessage: '新建告警规则' })
      : intl.formatMessage({ id: 'pages.alarm.modal.rule.title.edit', defaultMessage: '编辑告警规则' });

    const enabledChannelOptions = channels
      .filter((c) => c.id != null)
      .map((c) => ({
        label: c.channelType ? `${c.name} (${c.channelType})` : c.name,
        value: c.id as number,
        disabled: c.enabled === 0,
      }));

    const jobSelectOptions = jobOptions.map((j) => ({
      label: j.type === 'batch' ? `[离线] ${j.jobName}` : `[实时] ${j.jobName}`,
      value: j.id,
    }));

    return (
      <Modal
        width="56vw"
        open={open}
        centered
        maskClosable={false}
        onCancel={handleClose}
        destroyOnClose
        confirmLoading={confirmLoading}
        styles={{
          header: { padding: '20px 24px 16px', borderBottom: '1px solid #EEF2F6', marginBottom: 0 },
          body: { padding: '20px 24px 16px', background: '#F8FAFC', maxHeight: '69vh', overflowY: 'auto' },
          footer: { padding: '14px 24px 18px', borderTop: '1px solid #EEF2F6', background: '#FFFFFF', marginTop: 0 },
          content: { borderRadius: 20, overflow: 'hidden' },
        }}
        title={<div style={{ fontSize: 18, fontWeight: 600, color: '#101828' }}>{modalTitle}</div>}
        footer={
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
            <Button onClick={handleClose} style={{ height: 32, borderRadius: 16 }}>
              {intl.formatMessage({ id: 'pages.alarm.modal.cancel', defaultMessage: '取消' })}
            </Button>
            <Button
              type="primary"
              onClick={handleSubmit}
              loading={confirmLoading}
              style={{ height: 32, borderRadius: 16, paddingInline: 18 }}
            >
              {intl.formatMessage({ id: 'pages.alarm.modal.confirm', defaultMessage: '完成' })}
            </Button>
          </div>
        }
      >
        <Spin spinning={loadingJobs}>
          <Form form={form} layout="vertical">
            <Form.Item
              label={intl.formatMessage({ id: 'pages.alarm.field.name', defaultMessage: '规则名称' })}
              name="name"
              rules={[{ required: true, message: '请输入规则名称' }]}
            >
              <Input placeholder={intl.formatMessage({ id: 'pages.alarm.placeholder.ruleName', defaultMessage: '请输入规则名称' })} />
            </Form.Item>

            <Form.Item
              label={intl.formatMessage({ id: 'pages.alarm.field.jobDefinition', defaultMessage: '目标任务' })}
              name="targetJobs"
              tooltip={intl.formatMessage({ id: 'pages.alarm.field.jobDefinition.tooltip', defaultMessage: '不选择表示全部任务，选择多个表示仅对这些任务生效' })}
            >
              <Select
                mode="multiple"
                showSearch
                allowClear
                optionFilterProp="label"
                placeholder={intl.formatMessage({ id: 'pages.alarm.placeholder.jobDefinition', defaultMessage: '不选表示全部任务，可选择多个' })}
                options={jobSelectOptions}
              />
            </Form.Item>

            <Form.Item
              label={intl.formatMessage({ id: 'pages.alarm.field.triggerStatuses', defaultMessage: '触发状态' })}
              name="triggerStatuses"
              rules={[{ required: true, message: '请选择触发状态' }]}
            >
              <Select
                mode="multiple"
                allowClear
                placeholder={intl.formatMessage({ id: 'pages.alarm.placeholder.triggerStatuses', defaultMessage: '任务进入这些状态时触发告警' })}
                options={JOB_STATUS_OPTIONS}
              />
            </Form.Item>

            <div style={{ display: 'flex', gap: 16 }}>
              <Form.Item
                style={{ flex: 1 }}
                label={intl.formatMessage({ id: 'pages.alarm.field.severity', defaultMessage: '严重级别' })}
                name="severity"
                rules={[{ required: true, message: '请选择严重级别' }]}
              >
                <Select options={SEVERITY_OPTIONS} />
              </Form.Item>

              <Form.Item
                style={{ width: 120 }}
                label={intl.formatMessage({ id: 'pages.alarm.field.enabled', defaultMessage: '启用' })}
                name="enabled"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
            </div>

            <Form.Item
              label={intl.formatMessage({ id: 'pages.alarm.field.channelIds', defaultMessage: '告警通道' })}
              name="channelIds"
              tooltip={intl.formatMessage({ id: 'pages.alarm.field.channelIds.tooltip', defaultMessage: '可关联一个或多个通道，告警将同时投递' })}
            >
              <Select
                mode="multiple"
                allowClear
                placeholder={intl.formatMessage({ id: 'pages.alarm.placeholder.channelIds', defaultMessage: '请选择告警通道' })}
                options={enabledChannelOptions}
              />
            </Form.Item>

            <Form.Item noStyle shouldUpdate>
              {({ getFieldValue }) => {
                const selectedTargets: number[] = getFieldValue('targetJobs') || [];
                const excludeOptions = selectedTargets.length > 0
                  ? jobOptions.filter((j) => selectedTargets.includes(j.id)).map((j) => ({
                      label: j.type === 'batch' ? `[离线] ${j.jobName}` : `[实时] ${j.jobName}`,
                      value: j.id,
                    }))
                  : jobOptions.map((j) => ({
                      label: j.type === 'batch' ? `[离线] ${j.jobName}` : `[实时] ${j.jobName}`,
                      value: j.id,
                    }));
                return (
                  <Form.Item
                    label={intl.formatMessage({ id: 'pages.alarm.field.excludes', defaultMessage: '排除任务' })}
                    name="excludes"
                    tooltip={intl.formatMessage({ id: 'pages.alarm.field.excludes.tooltip', defaultMessage: '只能从已选目标任务中进行排除' })}
                  >
                    <Select
                      mode="multiple"
                      showSearch
                      allowClear
                      optionFilterProp="label"
                      placeholder={intl.formatMessage({ id: 'pages.alarm.placeholder.excludes', defaultMessage: '请选择需要排除的任务（仅限已选目标任务的子集）' })}
                      options={excludeOptions}
                    />
                  </Form.Item>
                );
              }}
            </Form.Item>

            <Form.Item
              label={intl.formatMessage({ id: 'pages.alarm.field.description', defaultMessage: '描述' })}
              name="description"
            >
              <TextArea rows={2} placeholder="请输入描述" />
            </Form.Item>
          </Form>
        </Spin>
      </Modal>
    );
  },
);

export default AddOrEditRuleModal;
