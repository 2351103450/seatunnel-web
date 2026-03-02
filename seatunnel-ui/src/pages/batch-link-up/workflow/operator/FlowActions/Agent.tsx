import { SyncOutlined } from "@ant-design/icons";
import { Sender, SenderProps } from "@ant-design/x";
import { Dropdown, Flex, GetRef, MenuProps, message } from "antd";
import React, { useEffect, useMemo, useRef, useState } from "react";

import { dataSourceApi, dataSourceCatalogApi } from "@/pages/data-source/type";
import DeepSeekIcon from "../../icon/DeepSeekIcon";

const Switch = Sender.Switch;

type Option = { label: string; value: string };

// ======================= AgentInfo（原样保留，可继续扩展） =======================
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
      {
        type: "text",
        value: "Perform a single table data synchronization from [",
      },
      {
        type: "select",
        key: "source_db",
        props: {
          options: ["MySQL", "PostgreSQL", "Oracle"],
          placeholder: "select source",
        },
      },
      { type: "text", value: "." },
      {
        type: "select",
        key: "source_table",
        props: {
          options: ["users", "orders", "products"],
          placeholder: "select source table",
        },
      },
      { type: "text", value: "] to [" },
      {
        type: "select",
        key: "sink_db",
        props: { options: ["Oracle", "MySQL"], placeholder: "select sink" },
      },
      { type: "text", value: "." },
      {
        type: "input",
        key: "sink_table",
        props: { placeholder: "enter sink table" },
      },
      { type: "text", value: "]." },
    ],

    zh_slotConfig: [
      { type: "text", value: "请将 [" },
      {
        type: "select",
        key: "source_db",
        props: {
          options: ["MySQL", "PostgreSQL", "Oracle"],
          placeholder: "选择源数据库",
        },
      },
      { type: "text", value: "] 中的 [" },
      {
        type: "select",
        key: "source_table",
        props: {
          options: ["users", "orders", "products"],
          placeholder: "选择源表",
        },
      },
      { type: "text", value: "] 表，全量同步至 " },
      {
        type: "select",
        key: "sink_db",
        props: { options: ["Oracle", "MySQL"], placeholder: "选择目标数据库" },
      },
      { type: "text", value: " 的 " },
      {
        type: "input",
        key: "sink_table",
        props: { placeholder: "输入目标表名" },
      },
      { type: "text", value: " 表。" },
    ],
  },
};

// ======================= slotConfig patch（一次 patch） =======================
function patchSlotConfig(
  slotConfig: any[],
  params: {
    sourceDbOptions: Array<string | Option>;
    sinkDbOptions: Array<string | Option>;
    tableOptions: string[];
    tableLoading: boolean;
    selectedSourceDb: string; // ✅ 新增
  }
) {
  const {
    sourceDbOptions,
    sinkDbOptions,
    tableOptions,
    tableLoading,
    selectedSourceDb,
  } = params;

  return slotConfig.map((item) => {
    if (item?.type === "select" && item?.key === "source_db") {
      return {
        ...item,
        value: selectedSourceDb, // ✅ 关键：把 state 写回去
        props: { ...item.props, options: sourceDbOptions },
      };
    }
    if (item?.type === "select" && item?.key === "sink_db") {
      return { ...item, props: { ...item.props, options: sinkDbOptions } };
    }
    if (item?.type === "select" && item?.key === "source_table") {
      return {
        ...item,
        props: {
          ...item.props,
          options: tableOptions,
          loading: tableLoading,
          placeholder: tableLoading
            ? "loading tables..."
            : item?.props?.placeholder,
        },
      };
    }
    return item;
  });
}
// ======================= hook：加载数据源 all() => options + map =======================
function useDataSources() {
  const [dbOptions, setDbOptions] = useState<string[]>([]);
  const [dbNameToId, setDbNameToId] = useState<Record<string, number>>({});

  useEffect(() => {
    dataSourceApi
      .all()
      .then((res: any) => {
        if (res?.code !== 0) {
          message.error(res?.message || "Load data sources failed");
          return;
        }

        const list = res?.data || [];
        const names: string[] = list.map((v: any) => v?.dbName).filter(Boolean);

        const map: Record<string, number> = {};
        list.forEach((v: any) => {
          if (v?.dbName != null && v?.id != null) {
            map[String(v.dbName)] = Number(v.id);
          }
        });

        setDbOptions(names);
        setDbNameToId(map);
      })
      .catch((err: any) => {
        console.error(err);
        message.error("Load data sources failed");
      });
  }, []);

  return { dbOptions, dbNameToId };
}

// ======================= hook：根据 selectedSourceDb 动态拉表（防竞态） =======================
function useTables(dbNameToId: Record<string, number>) {
  const [selectedSourceDb, setSelectedSourceDb] = useState<string>("");
  const [tableOptions, setTableOptions] = useState<string[]>([]);
  const [tableLoading, setTableLoading] = useState<boolean>(false);

  useEffect(() => {
    const dbName = selectedSourceDb;

    if (!dbName) {
      setTableOptions([]);
      return;
    }

    const datasourceId = dbNameToId[dbName];
    if (!datasourceId) {
      setTableOptions([]);
      return;
    }

    let cancelled = false;

    setTableLoading(true);
    setTableOptions([]); // 切换源库时先清空，避免显示旧表

    dataSourceCatalogApi
      .listTable(String(datasourceId))
      .then((res: any) => {
        if (cancelled) return;

        if (res?.code !== 0) {
          setTableOptions([]);
          message.error(res?.message || "Load tables failed");
          return;
        }

        const tables: string[] = (res?.data || [])
          .map((t: any) => (typeof t === "string" ? t : t?.value ?? t?.label))
          .filter(Boolean);

        setTableOptions(tables);
      })
      .catch((err: any) => {
        if (cancelled) return;
        console.error(err);
        setTableOptions([]);
        message.error("Load tables failed");
      })
      .finally(() => {
        if (cancelled) return;
        setTableLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [selectedSourceDb, dbNameToId]);

  return {
    selectedSourceDb,
    setSelectedSourceDb,
    tableOptions,
    tableLoading,
  };
}

// ======================= App =======================
const App: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [activeAgentKey, setActiveAgentKey] = useState("sync_copilot");
  const senderRef = useRef<GetRef<typeof Sender>>(null);

  const lastSourceDbRef = useRef<string>("");

  const { dbOptions, dbNameToId } = useDataSources();
  const { selectedSourceDb, setSelectedSourceDb, tableOptions, tableLoading } =
    useTables(dbNameToId);

  // agent 下拉
  const agentItems: MenuProps["items"] = useMemo(() => {
    return Object.keys(AgentInfo).map((agent) => {
      const { icon, label } = AgentInfo[agent];
      return { key: agent, icon, label };
    });
  }, []);

  // 最终 slotConfig：一次 patch
  const slotConfig = useMemo(() => {
    const base = AgentInfo[activeAgentKey];

    return {
      ...base,
      slotConfig: patchSlotConfig(base.slotConfig as any[], {
        sourceDbOptions: dbOptions,
        sinkDbOptions: dbOptions,
        tableOptions,
        tableLoading,
        selectedSourceDb, // ✅
      }),
      zh_slotConfig: patchSlotConfig(base.zh_slotConfig as any[], {
        sourceDbOptions: dbOptions,
        sinkDbOptions: dbOptions,
        tableOptions,
        tableLoading,
        selectedSourceDb, // ✅
      }),
    };
  }, [activeAgentKey, dbOptions, tableOptions, tableLoading, selectedSourceDb]);
  // loading 模拟
  useEffect(() => {
    if (!loading) return;
    const timer = setTimeout(() => {
      setLoading(false);
      message.success("Send message successfully!");
    }, 3000);
    return () => clearTimeout(timer);
  }, [loading]);

  function normalizeSlotValue(v: any): string {
    if (typeof v === "string") return v;
    if (v && typeof v === "object") {
      return String(v.value ?? v.label ?? "");
    }
    return "";
  }

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
        onChange={(_value, _event, slots) => {
          const raw = (slots?.find((x: any) => x.key === "source_db") as any)
            ?.value;
          const sourceDb = normalizeSlotValue(raw);

          // Sender 重建 slotConfig 时可能会发一次空值，忽略即可
          if (!sourceDb) return;

          // ✅ 用 state 去重：只在真的变了才 set
          if (sourceDb === selectedSourceDb) return;

          setSelectedSourceDb(sourceDb);
        }}
        footer={(actionNode) => (
          <Flex justify="space-between" align="center">
            <Flex gap="small" align="center">
              <Dropdown
                menu={{
                  selectedKeys: [activeAgentKey],
                  onClick: (item) => setActiveAgentKey(item.key),
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
