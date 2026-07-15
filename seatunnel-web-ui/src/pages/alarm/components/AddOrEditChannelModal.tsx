import { useIntl } from '@umijs/max';
import { Button, Form, Input, InputNumber, message, Modal, Select, Spin, Switch } from 'antd';
import React, { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react';
import { fetchChannelTypes, saveChannel, testChannel } from '../service';
import type {
  AlarmChannelCommand,
  AlarmChannelRecord,
  AlarmModalOpenPayload,
  AlarmModalRef,
  AlarmOperateType,
  ChannelFormValues,
  ChannelTypeVO,
  FormFieldConfig,
  FormFieldRule,
} from '../types';

const { TextArea } = Input;

/** 解析 configJson 为表单值，容错处理 */
function parseConfigJson(configJson?: string): Record<string, unknown> {
  if (!configJson) return {};
  try {
    const parsed = JSON.parse(configJson);
    return typeof parsed === 'object' && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

/** 后端 rules 转换为 antd Form.Item rules */
function toAntdRules(rules?: FormFieldRule[]) {
  if (!rules || rules.length === 0) return undefined;
  return rules.map((r) => {
    const rule: Record<string, unknown> = { message: r.message };
    if (r.required) rule.required = true;
    if (r.pattern) rule.pattern = new RegExp(r.pattern);
    if (r.min !== undefined) rule.min = r.min;
    if (r.max !== undefined) rule.max = r.max;
    return rule;
  });
}

/** 根据 FormFieldConfig.type 渲染对应的表单控件 */
function renderFormControl(field: FormFieldConfig) {
  switch (field.type) {
    case 'PASSWORD':
      return <Input.Password placeholder={field.placeholder} />;
    case 'SELECT':
    case 'CUSTOM_SELECT':
      return (
        <Select
          mode={field.type === 'CUSTOM_SELECT' ? 'tags' : undefined}
          allowClear
          placeholder={field.placeholder}
          options={field.options?.map((o) => ({ label: o.label, value: o.value }))}
        />
      );
    case 'NUMBER':
      return <InputNumber style={{ width: '100%' }} placeholder={field.placeholder} />;
    case 'SWITCH':
      return <Switch />;
    case 'TEXTAREA':
      return <TextArea rows={3} placeholder={field.placeholder} />;
    case 'INPUT':
    default:
      return <Input placeholder={field.placeholder} />;
  }
}

const AddOrEditChannelModal = forwardRef<AlarmModalRef>((_, ref) => {
  const intl = useIntl();
  const [basicForm] = Form.useForm<ChannelFormValues>();
  const [configForm] = Form.useForm();

  const [open, setOpen] = useState(false);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [loadingTypes, setLoadingTypes] = useState(false);
  const [operateType, setOperateType] = useState<AlarmOperateType>(
    'CREATE' as AlarmOperateType,
  );
  const [currentRecord, setCurrentRecord] = useState<AlarmChannelRecord>();
  const [channelTypes, setChannelTypes] = useState<ChannelTypeVO[]>([]);
  const [selectedType, setSelectedType] = useState<ChannelTypeVO | null>(null);
  const [showFormStep, setShowFormStep] = useState(false);
  const [testing, setTesting] = useState(false);

  const successCallbackRef = useRef<(() => void) | undefined>();

  const isCreateMode = operateType === ('CREATE' as AlarmOperateType);

  const resetState = () => {
    setCurrentRecord(undefined);
    setSelectedType(null);
    setShowFormStep(false);
    basicForm.resetFields();
    configForm.resetFields();
  };

  const handleClose = () => {
    setOpen(false);
    resetState();
  };

  const ensureChannelTypes = async (): Promise<ChannelTypeVO[]> => {
    if (channelTypes.length > 0) return channelTypes;
    setLoadingTypes(true);
    try {
      const res = await fetchChannelTypes();
      const list = res?.data || [];
      setChannelTypes(list);
      return list;
    } finally {
      setLoadingTypes(false);
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
      setCurrentRecord(nextRecord);
      successCallbackRef.current = onSuccess;

      const types = await ensureChannelTypes();

      // 编辑模式：直接定位通道类型并进入表单
      if (nextOperateType === ('EDIT' as AlarmOperateType) && nextRecord) {
        // 通道弹窗仅处理通道记录，收窄类型以访问通道专属字段
        const channelRecord = nextRecord as AlarmChannelRecord;
        const matched =
          types.find((t) => t.channelType === channelRecord.channelType) || null;
        setSelectedType(matched);
        setShowFormStep(true);

        basicForm.setFieldsValue({
          name: channelRecord.name || '',
          enabled: channelRecord.enabled !== 0,
          description: channelRecord.description || '',
        });

        const initialConfig = parseConfigJson(channelRecord.configJson);
        configForm.setFieldsValue(initialConfig);
        return;
      }

      // 创建模式：先进通道类型选择
      setSelectedType(null);
      setShowFormStep(false);
    },
    close: handleClose,
  }));

  // 选中通道类型后进入表单
  const handleSelectType = (type: ChannelTypeVO) => {
    basicForm.resetFields();
    configForm.resetFields();
    basicForm.setFieldsValue({ enabled: true });
    setSelectedType(type);
    setShowFormStep(true);
  };

  // 为动态字段设置默认值
  useEffect(() => {
    if (showFormStep && selectedType && isCreateMode) {
      const defaults: Record<string, unknown> = {};
      selectedType.configFields.forEach((f) => {
        if (f.defaultValue !== undefined && f.defaultValue !== null) {
          defaults[f.key] = f.defaultValue;
        }
      });
      configForm.setFieldsValue(defaults);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showFormStep, selectedType]);

  const handleTest = async () => {
    try {
      const configValues = await configForm.validateFields();
      setTesting(true);
      const res = await testChannel({
        channelType: selectedType?.channelType || '',
        configJson: JSON.stringify(configValues),
      });
      if (res?.code === 0 && res.data?.success) {
        message.success(res.data.message || '测试消息发送成功！');
      } else {
        message.error(res?.data?.message || '测试失败，请检查配置');
      }
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('测试请求失败');
    } finally {
      setTesting(false);
    }
  };

  const handleSubmit = async () => {
    try {
      const basicValues = await basicForm.validateFields();
      const configValues = await configForm.validateFields();

      const payload: AlarmChannelCommand = {
        name: basicValues.name,
        channelType: selectedType?.channelType,
        enabled: basicValues.enabled ? 1 : 0,
        description: basicValues.description,
        configJson: JSON.stringify(configValues),
      };

      if (!isCreateMode && currentRecord?.id) {
        payload.id = currentRecord.id;
      }

      setConfirmLoading(true);
      const res = await saveChannel(payload);

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
      if (error?.errorFields) return; // 表单校验失败
    } finally {
      setConfirmLoading(false);
    }
  };

  const modalTitle = isCreateMode
    ? intl.formatMessage({ id: 'pages.alarm.modal.channel.title.add', defaultMessage: '新建告警通道' })
    : intl.formatMessage({ id: 'pages.alarm.modal.channel.title.edit', defaultMessage: '编辑告警通道' });

  const sortedFields = selectedType?.configFields
    ? [...selectedType.configFields].sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
    : [];

  return (
    <Modal
      width="60vw"
      open={open}
      centered
      maskClosable={false}
      onCancel={handleClose}
      destroyOnClose
      confirmLoading={confirmLoading}
      styles={{
        header: { padding: '20px 24px 16px', borderBottom: '1px solid #EEF2F6', marginBottom: 0 },
        body: {
          padding: '20px 24px 16px',
          background: '#F8FAFC',
          maxHeight: '69vh',
          overflowY: 'auto',
          minHeight: '50vh',
        },
        footer: { padding: '14px 24px 18px', borderTop: '1px solid #EEF2F6', background: '#FFFFFF', marginTop: 0 },
        content: { borderRadius: 20, overflow: 'hidden' },
      }}
      title={
        <div style={{ fontSize: 18, fontWeight: 600, color: '#101828' }}>
          {modalTitle}
        </div>
      }
      footer={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
          {showFormStep && isCreateMode && (
            <Button
              onClick={() => {
                setShowFormStep(false);
                setSelectedType(null);
              }}
              style={{ height: 32, borderRadius: 16 }}
            >
              {intl.formatMessage({ id: 'pages.alarm.modal.channel.prev', defaultMessage: '上一步' })}
            </Button>
          )}
          <Button onClick={handleClose} style={{ height: 32, borderRadius: 16 }}>
            {intl.formatMessage({ id: 'pages.alarm.modal.cancel', defaultMessage: '取消' })}
          </Button>
          {showFormStep && (
            <>
              <Button
                onClick={handleTest}
                loading={testing}
                style={{ height: 32, borderRadius: 16, paddingInline: 18 }}
              >
                {intl.formatMessage({ id: 'pages.alarm.modal.channel.test', defaultMessage: '测试连通性' })}
              </Button>
              <Button
                type="primary"
                onClick={handleSubmit}
                loading={confirmLoading}
                style={{ height: 32, borderRadius: 16, paddingInline: 18 }}
              >
                {intl.formatMessage({ id: 'pages.alarm.modal.confirm', defaultMessage: '完成' })}
              </Button>
            </>
          )}
        </div>
      }
    >
      <Spin spinning={loadingTypes}>
        {showFormStep && selectedType ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Form form={basicForm} layout="vertical">
              <Form.Item
                label={intl.formatMessage({ id: 'pages.alarm.field.name', defaultMessage: '名称' })}
                name="name"
                rules={[{ required: true, message: '请输入名称' }]}
              >
                <Input placeholder={intl.formatMessage({ id: 'pages.alarm.placeholder.channelName', defaultMessage: '请输入通道名称' })} />
              </Form.Item>
              <Form.Item
                label={intl.formatMessage({ id: 'pages.alarm.field.enabled', defaultMessage: '启用' })}
                name="enabled"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
              <Form.Item
                label={intl.formatMessage({ id: 'pages.alarm.field.description', defaultMessage: '描述' })}
                name="description"
              >
                <TextArea rows={2} placeholder="请输入描述" />
              </Form.Item>
            </Form>

            {sortedFields.length > 0 && (
              <div>
                <div style={{ marginBottom: 8, fontSize: 14, fontWeight: 600, color: '#101828' }}>
                  {intl.formatMessage({ id: 'pages.alarm.modal.channel.config', defaultMessage: '通道配置' })}
                  <span style={{ marginLeft: 8, fontSize: 12, color: '#667085' }}>
                    ({selectedType.channelType})
                  </span>
                </div>
                <Form form={configForm} layout="vertical">
                  {sortedFields.map((field) => (
                    <Form.Item
                      key={field.key}
                      label={field.label}
                      name={field.key}
                      rules={toAntdRules(field.rules)}
                      valuePropName={field.type === 'SWITCH' ? 'checked' : undefined}
                    >
                      {renderFormControl(field)}
                    </Form.Item>
                  ))}
                </Form>
              </div>
            )}
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 12 }}>
            {channelTypes.map((type) => (
              <button
                type="button"
                key={type.channelType}
                onClick={() => handleSelectType(type)}
                style={{
                  textAlign: 'left',
                  padding: '16px',
                  borderRadius: 12,
                  border: '1px solid #EEF2F6',
                  background: '#FFFFFF',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.borderColor = 'hsl(231 48% 48%)';
                  e.currentTarget.style.boxShadow = '0 4px 12px hsl(231 48% 48% / 0.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.borderColor = '#EEF2F6';
                  e.currentTarget.style.boxShadow = 'none';
                }}
              >
                <div style={{ fontSize: 15, fontWeight: 600, color: '#101828' }}>
                  {type.displayName || type.channelType}
                </div>
                <div style={{ marginTop: 4, fontSize: 12, color: '#667085' }}>
                  {type.configFields.length} 个配置项
                </div>
              </button>
            ))}
            {channelTypes.length === 0 && !loadingTypes && (
              <div style={{ gridColumn: '1 / -1', textAlign: 'center', color: '#667085', padding: '40px 0' }}>
                {intl.formatMessage({ id: 'pages.alarm.modal.channel.noType', defaultMessage: '暂无可用的通道类型' })}
              </div>
            )}
          </div>
        )}
      </Spin>
    </Modal>
  );
});

export default AddOrEditChannelModal;
