import HttpUtils from "@/utils/HttpUtils";
import { LoadingOutlined, MinusCircleOutlined, PlusOutlined } from "@ant-design/icons";
import { Button, Form, Input, InputNumber, message, Select, Space, Switch } from "antd";
import TextArea from "antd/es/input/TextArea";
import { useEffect, useState } from "react";
import { useIntl } from "@umijs/max";
import { DynamicDataSourceFormProps, FormField, FormRule } from "./type";

const DynamicDataSourceForm: React.FC<DynamicDataSourceFormProps> = ({
  dbType,
  form,
  configForm,
  operateType,
}) => {
  const intl = useIntl();

  const [formConfig, setFormConfig] = useState<FormField[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    loadFormConfig();
  }, [dbType]);

  const loadFormConfig = async (): Promise<void> => {
    try {
      setLoading(true);
      const response = await HttpUtils.get<any>(
        `/api/v1/data-source/plugin/config?pluginType=${dbType}`,
      );
      if (response?.code === 0) {
        setFormConfig(response?.data?.formFields || []);
      } else {
        message.error(response?.message);
      }
    } catch (error) {
      message.error(
        intl.formatMessage({
          id: "pages.datasource.form.loadConfigFail",
          defaultMessage: "Failed to load form config",
        }),
      );
    } finally {
      setLoading(false);
    }
  };

  const renderFormItem = (field: FormField): React.ReactNode => {
    const commonProps = {
      placeholder: field.placeholder, // 这通常是后端下发的，如需国际化建议后端返回 i18nKey
      onChange: () => {
        setTimeout(() => {
          configForm.validateFields([field.key]).catch(() => {});
        }, 0);
      },
    };

    switch (field.type) {
      case "INPUT":
        return <Input {...commonProps} size="small" />;

      case "PASSWORD":
        return <Input.Password {...commonProps} size="small" />;

      case "SELECT":
        return (
          <Select {...commonProps} size="small">
            {field.options?.map((option) => (
              <Select.Option key={option.value} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>
        );

      case "NUMBER":
        return <InputNumber {...commonProps} size="small" />;

      case "SWITCH":
        // Switch 没有 placeholder，这里保持 commonProps 不影响（多余属性会被忽略）
        return <Switch {...commonProps} size="small" />;

      case "TEXTAREA":
        return <Input.TextArea rows={4} {...commonProps} size="small" />;

      default:
        return <Input {...commonProps} size="small" />;
    }
  };

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "24px ",
        }}
      >
        <LoadingOutlined />
      </div>
    );
  }

  return (
    <div style={{ padding: "0 16px" }}>
      <Form form={form} labelCol={{ span: 3 }} wrapperCol={{ span: 19 }}>
        <Form.Item
          label={
            <div style={{ height: 32, lineHeight: "33px" }}>
              {intl.formatMessage({
                id: "pages.datasource.form.dsName",
                defaultMessage: "DS Name",
              })}
            </div>
          }
          name="dbName"
          rules={[
            {
              required: true,
              message: intl.formatMessage({
                id: "pages.datasource.form.dsNameRequired",
                defaultMessage: "DS Name is required",
              }),
            },
          ]}
        >
          <Input
            placeholder={intl.formatMessage({
              id: "pages.datasource.form.inputPlaceholder",
              defaultMessage: "Input...",
            })}
            maxLength={100}
            size="small"
          />
        </Form.Item>

        <Form.Item
          label={
            <div style={{ height: 32, lineHeight: "33px" }}>
              {intl.formatMessage({
                id: "pages.datasource.form.env",
                defaultMessage: "Env",
              })}
            </div>
          }
          name="environment"
          rules={[
            {
              required: true,
              message: intl.formatMessage({
                id: "pages.datasource.form.envRequired",
                defaultMessage: "Env is required",
              }),
            },
          ]}
        >
          <Select
            placeholder={intl.formatMessage({
              id: "pages.datasource.form.selectPlaceholder",
              defaultMessage: "Select...",
            })}
            size="small"
            options={[
              { label: "DEVELOP", value: "DEVELOP" },
              { label: "TEST", value: "TEST" },
              { label: "PROD", value: "PROD" },
            ]}
          />
        </Form.Item>

        <Form.Item
          label={
            <div style={{ height: 32, lineHeight: "33px" }}>
              {intl.formatMessage({
                id: "pages.datasource.form.description",
                defaultMessage: "Description",
              })}
            </div>
          }
          name="remark"
        >
          <TextArea
            placeholder={intl.formatMessage({
              id: "pages.datasource.form.inputPlaceholder",
              defaultMessage: "Input...",
            })}
            size="small"
            rows={4}
          />
        </Form.Item>

        <Form.Item name="connectionParams" hidden>
          <Input type="hidden" />
        </Form.Item>

        <Form form={configForm} initialValues={getConfigInitialValues(formConfig)} component={false}>
          {formConfig.map((field) => {
            if (field.type === "CUSTOM_SELECT") {
              return (
                <div key={field.key} style={{ paddingLeft: "113px" }}>
                  <Form.List name="other">
                    {(fields, { add, remove }) => (
                      <>
                        {fields.map(({ key, name, ...restField }) => (
                          <Space
                            key={key}
                            style={{ display: "flex", marginBottom: 8 }}
                            align="baseline"
                          >
                            <Form.Item
                              {...restField}
                              name={[name, "key"]}
                              style={{ marginBottom: 0 }}
                              rules={[
                                {
                                  required: true,
                                  message: intl.formatMessage({
                                    id: "pages.datasource.form.other.keyRequired",
                                    defaultMessage: "key can not be null",
                                  }),
                                },
                              ]}
                            >
                              <Input
                                placeholder={intl.formatMessage({
                                  id: "pages.datasource.form.other.keyPlaceholder",
                                  defaultMessage: "key",
                                })}
                                size="small"
                                style={{ width: "310px" }}
                              />
                            </Form.Item>

                            <Form.Item
                              {...restField}
                              style={{ marginBottom: 0 }}
                              name={[name, "value"]}
                              rules={[
                                {
                                  required: true,
                                  message: intl.formatMessage({
                                    id: "pages.datasource.form.other.valueRequired",
                                    defaultMessage: "value can not be null",
                                  }),
                                },
                              ]}
                            >
                              <Input
                                placeholder={intl.formatMessage({
                                  id: "pages.datasource.form.other.valuePlaceholder",
                                  defaultMessage: "value",
                                })}
                                size="small"
                                style={{ width: "310px" }}
                              />
                            </Form.Item>

                            <MinusCircleOutlined onClick={() => remove(name)} />
                          </Space>
                        ))}

                        <Form.Item>
                          <Button
                            type="dashed"
                            size="small"
                            style={{ width: "650px" }}
                            onClick={() => add()}
                            block
                            icon={<PlusOutlined />}
                          >
                            {intl.formatMessage({
                              id: "pages.datasource.form.other.addConnSetting",
                              defaultMessage: "Add Database Connection Settings",
                            })}
                          </Button>
                        </Form.Item>
                      </>
                    )}
                  </Form.List>
                </div>
              );
            }

            return (
              <Form.Item
                labelCol={{ span: 3 }}
                wrapperCol={{ span: 19 }}
                key={field.key}
                label={<div style={{ height: 32, lineHeight: "33px" }}>{field.label}</div>}
                name={field.key}
                rules={transformRules(field?.rules)}
                validateTrigger={["onChange", "onBlur"]}
              >
                {renderFormItem(field)}
              </Form.Item>
            );
          })}
        </Form>
      </Form>
    </div>
  );
};

// 转换验证规则（后端下发的 rule.message 这里保持原样）
const transformRules = (rules: FormRule[] | undefined): any[] => {
  if (!rules) return [];
  return rules.map((rule) => {
    const formRule: any = { message: rule.message };
    if (rule.required) {
      formRule.required = true;
    }
    return formRule;
  });
};

// 设置配置表单的初始值
const getConfigInitialValues = (formConfig: FormField[]): Record<string, any> => {
  const initialValues: Record<string, any> = {};
  formConfig.forEach((field) => {
    if (field.defaultValue !== undefined) {
      initialValues[field.key] = field.defaultValue;
    }
  });
  return initialValues;
};

export default DynamicDataSourceForm;