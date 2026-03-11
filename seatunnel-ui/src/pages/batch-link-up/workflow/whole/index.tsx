import Header from "@/components/Header";
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
      <div style={{ overflow: "auto", height: "calc(100vh - 190px)" }}>
        <div style={{ margin: 16 }}>
          <div style={{ backgroundColor: "white", padding: "16px 24px" }}>
            <Header title="数据表设置" />
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
                    fetchSize: 8000,
                    splitSize: 8096,
                    dataSaveMode: "APPEND_DATA",
                    schemaSaveMode: "CREATE_SCHEMA_WHEN_NOT_EXIST",
                    enableUpsert: true,
                  }}
                  layout="vertical"
                >
                  <Form.Item
                    label="每次拉取行数（Fetch Size）"
                    name="fetchSize"
                    tooltip={
                      <>
                        <div>每次从数据库读取的记录数量。</div>
                        <div>默认值：0（使用 JDBC 默认值）</div>
                        <div style={{ marginTop: 6 }}>建议设置：</div>
                        <div>• 小表（&lt;10万行）：500 - 1000</div>
                        <div>• 中表（10万 ~ 1000万）：2000 - 5000</div>
                        <div>• 大表（&gt;1000万）：5000 - 10000</div>
                      </>
                    }
                  >
                    <InputNumber
                      min={0}
                      style={{ width: "100%" }}
                      size="small"
                      placeholder="0"
                    />
                  </Form.Item>

                  <Form.Item
                    label="读取分片大小（Split Size）"
                    name="splitSize"
                    tooltip={
                      <>
                        <div>读取表数据时每个分片包含的行数。</div>
                        <div>默认值：8096</div>
                        <div style={{ marginTop: 6 }}>建议设置：</div>
                        <div>• 小表（&lt;10万行）：2000</div>
                        <div>• 中表（10万 ~ 1000万）：8000</div>
                        <div>• 大表（&gt;1000万）：20000</div>
                      </>
                    }
                  >
                    <InputNumber
                      min={1}
                      style={{ width: "100%" }}
                      size="small"
                      placeholder="8096"
                    />
                  </Form.Item>
                </Form>
              </Col>
              <Col span={12}>
                <Form
                  form={form}
                  layout="vertical"
                  initialValues={{
                    schemaSaveMode: "CREATE_SCHEMA_WHEN_NOT_EXIST",
                    dataSaveMode: "APPEND_DATA",
                    batchSize: 10000,
                    enableUpsert: true,
                    fieldIde: "ORIGINAL",
                  }}
                >
                  <Row gutter={[16, 4]}>
                    <Col span={12}>
                      <Form.Item
                        label="Schema 保存模式"
                        name="schemaSaveMode"
                        rules={[
                          { required: true, message: "请选择 Schema 保存模式" },
                        ]}
                        tooltip={
                          <>
                            <div>
                              控制目标表结构不存在或需要调整时的处理方式。
                            </div>
                            <div>默认值：不存在则创建</div>
                            <div style={{ marginTop: 6 }}>可选说明：</div>
                            <div>• 不存在则创建：目标表不存在时自动创建</div>
                            <div>• 重新创建：先删除再重建目标表结构</div>
                            <div>• 不存在则报错：目标表不存在时任务失败</div>
                            <div>• 忽略：不处理表结构</div>
                          </>
                        }
                      >
                        <Select
                          size="small"
                          placeholder="请选择"
                          options={[
                            {
                              label: "不存在则创建",
                              value: "CREATE_SCHEMA_WHEN_NOT_EXIST",
                            },
                            {
                              label: "重新创建",
                              value: "RECREATE_SCHEMA",
                            },
                            {
                              label: "不存在则报错",
                              value: "ERROR_WHEN_SCHEMA_NOT_EXIST",
                            },
                            {
                              label: "忽略",
                              value: "IGNORE",
                            },
                          ]}
                        />
                      </Form.Item>
                    </Col>

                    <Col span={12}>
                      <Form.Item
                        label="数据保存模式"
                        name="dataSaveMode"
                        rules={[
                          { required: true, message: "请选择数据保存模式" },
                        ]}
                        tooltip={
                          <>
                            <div>控制写入目标表时对已有数据的处理方式。</div>
                            <div>默认值：追加数据</div>
                            <div style={{ marginTop: 6 }}>可选说明：</div>
                            <div>• 追加数据：保留已有数据，在后面继续写入</div>
                            <div>
                              • 清空后写入：先删除目标表数据，再重新写入
                            </div>
                          </>
                        }
                      >
                        <Select
                          size="small"
                          placeholder="请选择"
                          options={[
                            {
                              label: "追加数据",
                              value: "APPEND_DATA",
                            },
                            {
                              label: "清空后写入",
                              value: "DROP_DATA",
                            },
                          ]}
                        />
                      </Form.Item>
                    </Col>

                    <Col span={12}>
                      <Form.Item
                        label="启用 Upsert"
                        name="enableUpsert"
                        valuePropName="checked"
                        tooltip={
                          <>
                            <div>
                              开启后，目标端存在相同主键或唯一键时执行更新，否则插入。
                            </div>
                            <div>默认值：开启</div>
                            <div style={{ marginTop: 6 }}>适用场景：</div>
                            <div>• 需要按主键更新已有数据</div>
                            <div>• 需要避免重复插入</div>
                            <div style={{ marginTop: 6 }}>
                              使用前请确认目标表支持主键、唯一键或对应的 Upsert
                              能力。
                            </div>
                          </>
                        }
                      >
                        <Switch />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Divider style={{ margin: "8px 0 16px" }} />

                  <Row gutter={[16, 4]}>
                    <Col span={12}>
                      <Form.Item
                        label="批次大小"
                        name="batchSize"
                        rules={[{ required: true, message: "请输入批次大小" }]}
                        tooltip={
                          <>
                            <div>每批次写入目标端的数据条数。</div>
                            <div>默认值：10000</div>
                            <div style={{ marginTop: 6 }}>建议设置：</div>
                            <div>• 小表（&lt;10万行）：1000 - 5000</div>
                            <div>• 中表（10万 ~ 1000万）：5000 - 10000</div>
                            <div>• 大表（&gt;1000万）：10000 - 50000</div>
                            <div style={{ marginTop: 6 }}>
                              数值越大，吞吐通常越高，但也会增加内存和写入压力。
                            </div>
                          </>
                        }
                      >
                        <InputNumber
                          min={1}
                          placeholder="默认 10000"
                          size="small"
                          style={{ width: "100%" }}
                        />
                      </Form.Item>
                    </Col>

                    <Col span={12}>
                      <Form.Item
                        label="字段标识格式"
                        name="fieldIde"
                        tooltip={
                          <>
                            <div>控制字段名写入目标端时的大小写格式。</div>
                            <div>默认值：保持原样</div>
                            <div style={{ marginTop: 6 }}>可选说明：</div>
                            <div>• 保持原样：按源字段名原样写入</div>
                            <div>• 转大写：字段名统一转为大写</div>
                            <div>• 转小写：字段名统一转为小写</div>
                            <div style={{ marginTop: 6 }}>
                              适用于跨库同步时字段大小写敏感或命名规范不一致的场景。
                            </div>
                          </>
                        }
                      >
                        <Select
                          size="small"
                          placeholder="请选择"
                          options={[
                            { label: "保持原样", value: "ORIGINAL" },
                            { label: "转大写", value: "UPPERCASE" },
                            { label: "转小写", value: "LOWERCASE" },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                  </Row>
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
