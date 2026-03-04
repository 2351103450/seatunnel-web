import Header from "@/components/Header";
import { Form, Input, Switch } from "antd";
import TextArea from "antd/es/input/TextArea";
import { useIntl } from "@umijs/max";

const BasicConfig = () => {
  const intl = useIntl();

  return (
    <>
      <Header
        title={
          <span style={{ fontSize: 12, fontWeight: 500 }}>
            {intl.formatMessage({
              id: "pages.job.config.basicSetting",
              defaultMessage: "Basic Setting",
            })}
          </span>
        }
      />

      <Form.Item
        label={intl.formatMessage({
          id: "pages.job.config.jobName",
          defaultMessage: "Job Name",
        })}
        name="jobName"
        rules={[
          {
            required: true,
            message: intl.formatMessage({
              id: "pages.job.config.jobName.required",
              defaultMessage: "Job name cannot be empty",
            }),
          },
        ]}
      >
        <Input size="small" />
      </Form.Item>

      <Form.Item
        label={intl.formatMessage({
          id: "pages.job.config.jobDesc",
          defaultMessage: "Job Description",
        })}
        name="jobDesc"
      >
        <TextArea rows={4} size="small" />
      </Form.Item>

      <Form.Item
        label={intl.formatMessage({
          id: "pages.job.config.multiSync",
          defaultMessage: "Multi Sync",
        })}
        name="wholeSync"
        valuePropName="checked"
      >
        <Switch />
      </Form.Item>
    </>
  );
};

export default BasicConfig;