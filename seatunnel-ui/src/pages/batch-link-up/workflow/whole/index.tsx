import Header from "@/components/Header";
import ExtraParamsConfig from "@/pages/stream-link-up/detail/components/ExtraParamsConfig";
import { useLocation } from "@umijs/max";
import { Col, Divider, Form, InputNumber, Row, Select, Switch } from "antd";
import SyncTitle from "./SyncTitle";
import ReferenceTablePanel from "./components/ReferenceTablePanel";
import TableTransferPanel from "./components/TableTransferPanel";
import WholeSyncActions from "./components/WholeSyncActions";
import WholeSyncForm from "./components/WholeSyncForm";
import WholeSyncHeader from "./components/WholeSyncHeader";
import { useWholeSync } from "./hooks/useWholeSync";
import "./index.less";

const WholeSync = ({ goBack, baseForm }) => {
  const [form] = Form.useForm();

  const {
    loading,
    data,
    multiTableList,
    setMultiTableList,
    readOnlyTables,
    sourceType,
    targetType,
    sourceOption,
    targetOption,
    matchMode,
    tableKeyword,
    handleSourceTypeChange,
    handleTargetTypeChange,
    handleSourceIdChange,
    handleMatchModeChange,
    handleKeywordChange,
    buildTaskDraft,
  } = useWholeSync({ baseForm, form });

  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const idFromUrl = query.get("id");

  return (
    <>
      <WholeSyncHeader
        sourceType={sourceType}
        targetType={targetType}
        onSourceChange={handleSourceTypeChange}
        onTargetChange={handleTargetTypeChange}
        goBack={goBack}
      />
      <div style={{overflow: "auto",height: "calc(100vh - 190px)"}}>
        <div style={{ margin: 16 }}>
          <div style={{ backgroundColor: "white", padding: "16px 24px" }}>
            <Header title="表设置" />
            <SyncTitle />

            <WholeSyncForm
              form={form}
              sourceOption={sourceOption}
              targetOption={targetOption}
              matchMode={matchMode}
              tableKeyword={tableKeyword}
              onSourceIdChange={handleSourceIdChange}
              onMatchModeChange={handleMatchModeChange}
              onKeywordChange={handleKeywordChange}
            />

            {(matchMode === "1" || matchMode === "4") && (
              <TableTransferPanel
                loading={loading}
                data={data}
                targetKeys={multiTableList}
                matchMode={matchMode}
                onChange={setMultiTableList}
              />
            )}

            {(matchMode === "2" || matchMode === "3") && (
              <ReferenceTablePanel loading={loading} data={readOnlyTables} />
            )}
          </div>
        </div>

        <div
          style={{
            margin: "16px 16px 0 16px",
            backgroundColor: "white",
            padding: "16px 24px",
            marginBottom: "4vh",
          }}
        >
          <Header title={"参数设置"} />
          <SyncTitle />
          <div>
            <Row gutter={24}>
              <Col span={12}>
                <Form
                  form={form}
                  initialValues={{
                    startupMode: "INITIAL",
                    stopMode: "NEVER",
                    schemaChange: true,
                    batchSize: 10000,
                    exactlyOnce: false,
                    dataSaveMode: "APPEND_DATA",
                    schemaSaveMode: "CREATE_SCHEMA_WHEN_NOT_EXIST",
                    enableUpsert: true,
                  }}
                  layout="vertical"
                >
                  <Form.Item
                    label="Startup Mode"
                    name="startupMode"
                    labelCol={{ span: 4 }}
                    wrapperCol={{ span: 19 }}
                    rules={[{ required: true }]}
                  >
                    <Select
                      size="small"
                      placeholder="Select..."
                      options={[
                        {
                          label: "EARLIEST",
                          value: "EARLIEST",
                        },
                        {
                          label: "LATEST",
                          value: "LATEST",
                        },
                        {
                          label: "INITIAL",
                          value: "INITIAL",
                        },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item
                    label="Stoptup Mode"
                    name="stopMode"
                    labelCol={{ span: 4 }}
                    wrapperCol={{ span: 19 }}
                    rules={[{ required: true }]}
                  >
                    <Select
                      placeholder="Select..."
                      size="small"
                      options={[
                        {
                          label: "LATEST",
                          value: "LATEST",
                        },
                        {
                          label: "NEVER",
                          value: "NEVER",
                        },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item
                    label="Schema Change"
                    name="schemaChange"
                    rules={[{ required: true }]}
                  >
                    <Switch />
                  </Form.Item>
                  <Divider style={{ padding: 0, margin: "10px 0" }} />
                  <Form.Item label="Extra Params" name="sourceExtraParams">
                    <ExtraParamsConfig pluginName={sourceType?.pluginName} />
                  </Form.Item>
                </Form>
              </Col>
              <Col span={12}>
                <Form form={form} initialValues={{}} layout="vertical">
                  <Row gutter={24}>
                    <Col span={12}>
                      <Form.Item
                        label="Schema Save Mode"
                        name="schemaSaveMode"
                        rules={[{ required: true }]}
                      >
                        <Select
                          size="small"
                          showSearch
                          placeholder="Select..."
                          options={[
                            {
                              label: "CREATE IF MISSING",
                              value: "CREATE_SCHEMA_WHEN_NOT_EXIST",
                            },
                            { label: "RECREATE", value: "RECREATE_SCHEMA" },
                            {
                              label: "ERROR IF MISSING",
                              value: "ERROR_WHEN_SCHEMA_NOT_EXIST",
                            },
                            { label: "IGNORE", value: "IGNORE" },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        label="Data Save Mode"
                        name="dataSaveMode"
                        rules={[{ required: true }]}
                      >
                        <Select
                          size="small"
                          showSearch
                          placeholder="Select..."
                          options={[
                            {
                              label: "APPEND DATA",
                              value: "APPEND_DATA",
                            },
                            {
                              label: "DROP DATA",
                              value: "DROP_DATA",
                            },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Form.Item
                    label="Batch Size"
                    name="batchSize"
                    labelCol={{ span: 4 }}
                    wrapperCol={{ span: 19 }}
                    rules={[{ required: true }]}
                  >
                    <InputNumber
                      placeholder="Input..."
                      size="small"
                      style={{ width: "30%" }}
                    />
                  </Form.Item>
                  <Row gutter={24}>
                    <Col span={12}>
                      <Form.Item
                        label="Exactly Once"
                        name="exactlyOnce"
                        rules={[{ required: true }]}
                      >
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        label="Upsert"
                        name="enableUpsert"
                        rules={[{ required: true }]}
                      >
                        <Switch />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Divider style={{ padding: 0, margin: "6px 0" }} />

                  <Form.Item label="Extra Params" name="sinkExtraParams">
                    <ExtraParamsConfig pluginName={targetType?.pluginName} />
                  </Form.Item>
                </Form>
              </Col>
            </Row>
          </div>
        </div>
      </div>

      <WholeSyncActions
        form={form}
        baseForm={baseForm}
        goBack={goBack}
        idFromUrl={idFromUrl}
        sourceType={sourceType}
        targetType={targetType}
        matchMode={matchMode}
        multiTableList={multiTableList}
        buildTaskDraft={buildTaskDraft}
      />
    </>
  );
};

export default WholeSync;
