import React, { useEffect, useMemo, useRef, useState } from "react";
import { SyncOutlined } from "@ant-design/icons";
import { Sender, SenderProps } from "@ant-design/x";
import { Dropdown, Flex, GetRef, MenuProps, message } from "antd";
import DeepSeekIcon from "../../icon/DeepSeekIcon";
import { dataSourceApi, dataSourceCatalogApi } from "@/pages/data-source/type";

const Switch = Sender.Switch;

type Option = { label: string; value: string };

// ======================= AgentInfo（原样保留） =======================
const AgentInfo: {
  [key: string]: {
    icon: React.ReactNode;
    label: string;
    zh_label: string;
    skill: SenderProps["skill"];
    zh_skill: SenderProps["skill"];
    slotConfig: SenderProps["slotConfig"];
    zh_slotConfig: SenderProps["slotConfig"];
  };
} = {
  sync_copilot: {
    icon: <SyncOutlined />,
    label: "SINGLE SYNC🧠",
    zh_label: "同步助手",

    skill: {
      value: "SINGLE_SYNC",
      title: "SINGLE SYNC🧠",
      closable: true,
    },

    zh_skill: {
      value: "SINGLE_SYNC",
      title: "SINGLE SYNC🧠",
      closable: true,
    },

    slotConfig: [
      { type: "text", value: "Perform a single table data synchronization from [" },
      {
        type: "select",
        key: "source_db",
        props: { options: ["MySQL", "PostgreSQL", "Oracle"], placeholder: "select source" },
      },
      { type: "text", value: "." },
      {
        type: "select",
        key: "source_table",
        props: { options: ["users", "orders", "products"], placeholder: "select source table" },
      },
      { type: "text", value: "] to [" },
      {
        type: "select",
        key: "sink_db",
        props: { options: ["Oracle", "MySQL"], placeholder: "select sink" },
      },
      { type: "text", value: "." },
      { type: "input", key: "sink_table", props: { placeholder: "enter sink table" } },
      { type: "text", value: "]." },
    ],

    zh_slotConfig: [
      { type: "text", value: "请将 [" },
      {
        type: "select",
        key: "source_db",
        props: { options: ["MySQL", "PostgreSQL", "Oracle"], placeholder: "选择源数据库" },
      },
      { type: "text", value: "] 中的 [" },
      {
        type: "select",
        key: "source_table",
        props: { options: ["users", "orders", "products"], placeholder: "选择源表" },
      },
      { type: "text", value: "] 表，全量同步至 " },
      {
        type: "select",
        key: "sink_db",
        props: { options: ["Oracle", "MySQL"], placeholder: "选择目标数据库" },
      },
      { type: "text", value: " 的 " },
      { type: "input", key: "sink_table", props: { placeholder: "输入目标表名" } },
      { type: "text", value: " 表。" },
    ],
  },
};

// ======================= patch 工具函数（组件外，避免重建） =======================
function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj));
}

function applyDbOptions(
  agent: (typeof AgentInfo)[keyof typeof AgentInfo],
  sourceOpts: Array<string | Option>,
  sinkOpts: Array<string | Option>
) {
  const next = deepClone(agent);

  const patch = (arr: any[]) =>
    arr.map((item) => {
      if (item?.type === "select" && item?.key === "source_db") {
        return { ...item, props: { ...item.props, options: sourceOpts } };
      }
      if (item?.type === "select" && item?.key === "sink_db") {
        return { ...item, props: { ...item.props, options: sinkOpts } };
      }
      return item;
    });

  next.slotConfig = patch(next.slotConfig);
  next.zh_slotConfig = patch(next.zh_slotConfig);
  return next;
}

function applyTableOptions(
  agent: (typeof AgentInfo)[keyof typeof AgentInfo],
  tableOpts: string[],
  loading?: boolean
) {
  const next = deepClone(agent);

  const patch = (arr: any[]) =>
    arr.map((item) => {
      if (item?.type === "select" && item?.key === "source_table") {
        return {
          ...item,
          props: {
            ...item.props,
            options: tableOpts,
            // 某些版本可能不支持 loading，但不会影响
            loading: !!loading,
            placeholder: loading ? "loading tables..." : item?.props?.placeholder,
          },
        };
      }
      return item;
    });

  next.slotConfig = patch(next.slotConfig);
  next.zh_slotConfig = patch(next.zh_slotConfig);
  return next;
}

// 从拼接后的 value 里解析 source_db（退化方案，非常稳）
function parseSourceDbFromValue(value: string) {
  // 模板：from [<source_db>.<source_table>] to [...]
  const m = value?.match(/from\s*\[\s*([^. \]]+)\s*\./i);
  return m?.[1] || "";
}

const App: React.FC = () => {
  // ======================= state 一律放最上面（避免 TDZ） =======================
  const [loading, setLoading] = useState<boolean>(false);
  const [activeAgentKey, setActiveAgentKey] = useState("sync_copilot");

  const [sourceDbOptions, setSourceDbOptions] = useState<string[]>([]);
  const [sinkDbOptions, setSinkDbOptions] = useState<string[]>([]);
  const [dbNameToId, setDbNameToId] = useState<Record<string, number>>({});

  const [selectedSourceDb, setSelectedSourceDb] = useState<string>("");
  const [tableOptions, setTableOptions] = useState<string[]>([]);
  const [tableLoading, setTableLoading] = useState<boolean>(false);

  const senderRef = useRef<GetRef<typeof Sender>>(null);

  // ======================= agent 下拉 =======================
  const agentItems: MenuProps["items"] = useMemo(() => {
    return Object.keys(AgentInfo).map((agent) => {
      const { icon, label } = AgentInfo[agent];
      return { key: agent, icon, label };
    });
  }, []);

  const agentItemClick: MenuProps["onClick"] = (item) => {
    setActiveAgentKey(item.key);
  };

  // ======================= 1）加载数据源 all() => options + map =======================
  useEffect(() => {
    dataSourceApi
      .all()
      .then((res: any) => {
        if (res?.code === 0) {
          const list = res?.data || [];

          const names: string[] = list.map((v: any) => v?.dbName).filter(Boolean);

          setSourceDbOptions(names);
          setSinkDbOptions(names);

          const map: Record<string, number> = {};
          list.forEach((v: any) => {
            if (v?.dbName != null && v?.id != null) {
              map[String(v.dbName)] = Number(v.id);
            }
          });
          setDbNameToId(map);
        } else {
          message.error(res?.message || "");
        }
      })
      .catch((err: any) => {
        console.error(err);
        message.error("Load data sources failed");
      });
  }, []);

  // ======================= 2）根据 selectedSourceDb 动态拉表 =======================
  useEffect(() => {
    if (!selectedSourceDb) {
      setTableOptions([]);
      return;
    }

    const datasourceId = dbNameToId[selectedSourceDb];
    if (!datasourceId) {
      setTableOptions([]);
      return;
    }

    setTableLoading(true);
    setTableOptions([]); // 切换源库时先清空，避免显示旧表

    dataSourceCatalogApi
      .listTable(String(datasourceId))
      .then((res: any) => {
        if (res?.code === 0) {
          console.log(res?.data);
          const tables: string[] = (res?.data || [])
            .map((t: any) => (typeof t === "string" ? t : t?.value ?? t?.label))
            .filter(Boolean);
            
          setTableOptions(tables);
        } else {
          setTableOptions([]);
          message.error(res?.message || "");
        }
      })
      .catch((err: any) => {
        console.error(err);
        setTableOptions([]);
        message.error("Load tables failed");
      })
      .finally(() => setTableLoading(false));
  }, [selectedSourceDb, dbNameToId]);

  // ======================= 3）最终 slotConfig：注入 db options + table options =======================
  const slotConfig = useMemo(() => {
    const base = AgentInfo[activeAgentKey];
    let next = deepClone(base);

    if (sourceDbOptions.length || sinkDbOptions.length) {
      next = applyDbOptions(next, sourceDbOptions, sinkDbOptions);
    }

    next = applyTableOptions(next, tableOptions, tableLoading);
    return next;
  }, [activeAgentKey, sourceDbOptions, sinkDbOptions, tableOptions, tableLoading]);

  // ======================= loading 模拟 =======================
  useEffect(() => {
    if (loading) {
      const timer = setTimeout(() => {
        setLoading(false);
        message.success("Send message successfully!");
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [loading]);

  return (
    <Flex vertical gap="middle">
      <Sender
        loading={loading}
        ref={senderRef}
        skill={slotConfig.skill}
        placeholder="Press Enter to send message"
        slotConfig={slotConfig.slotConfig}
        autoSize={{ minRows: 3, maxRows: 6 }}
        suffix={false}
        footer={(actionNode) => (
          <Flex justify="space-between" align="center">
            <Flex gap="small" align="center">
              <Dropdown
                menu={{
                  selectedKeys: [activeAgentKey],
                  onClick: agentItemClick,
                  items: agentItems,
                }}
              >
                <Switch
                  value={false}
                  icon={<DeepSeekIcon style={{ paddingTop: 4 }} />}
                  style={{ borderRadius: 12 }}
                >
                  Copilot
                </Switch>
              </Dropdown>
            </Flex>
            <Flex align="center">{actionNode}</Flex>
          </Flex>
        )}
        onChange={(value, event) => {
          // 方式1：优先从 event 里拿结构化 values（不同版本字段名可能不同）
          const values =
            (event as any)?.values ||
            (event as any)?.slotValues ||
            (event as any)?.slots ||
            (event as any)?.data?.values;

          const sourceDbFromEvent = values?.source_db;
          if (typeof sourceDbFromEvent === "string" && sourceDbFromEvent) {
            // 只有变化才 set，减少 effect 触发
            setSelectedSourceDb((prev) => (prev === sourceDbFromEvent ? prev : sourceDbFromEvent));
            return;
          }

          // 方式2：退化解析（你的模板很固定，解析很稳）
          const parsed = parseSourceDbFromValue(value);
          if (parsed) {
            setSelectedSourceDb((prev) => (prev === parsed ? prev : parsed));
          }
        }}
        onSubmit={(v, _, skill) => {
          setLoading(true);
          message.info(`Send message: ${skill?.value} | ${v}`);
          senderRef.current?.clear?.();
        }}
        onCancel={() => {
          setLoading(false);
          message.error("Cancel sending!");
        }}
      />
    </Flex>
  );
};

export default () => <App />;