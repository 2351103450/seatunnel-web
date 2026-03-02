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

function patchSlotConfig(
  slotConfig: any[],
  params: {
    sourceDbOptions: Array<string | Option>;
    sinkDbOptions: Array<string | Option>;
    tableOptions: string[];
    tableLoading: boolean;
    selectedSourceDb: string;
  }
) {
  const {
    sourceDbOptions,
    sinkDbOptions,
    tableOptions,
    tableLoading,
    selectedSourceDb,
  } = params;
  console.log(slotConfig);
  return slotConfig.map((item) => {
    if (item?.type === "select" && item?.key === "source_db") {
      return {
        ...item,
        // value: selectedSourceDb,
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

function useTables(dbNameToId: Record<string, number>) {
  const [selectedSourceDb, setSelectedSourceDb] = useState("");
  const [tablesState, setTablesState] = useState<{
    loading: boolean;
    options: string[];
  }>({ loading: false, options: [] });

  useEffect(() => {
    const dbName = selectedSourceDb;
    if (!dbName) {
      setTablesState({ loading: false, options: [] });
      return;
    }

    const datasourceId = dbNameToId[dbName];
    if (!datasourceId) {
      setTablesState({ loading: false, options: [] });
      return;
    }

    let cancelled = false;

    setTablesState({ loading: true, options: [] });

    dataSourceCatalogApi
      .listTable(String(datasourceId))
      .then((res: any) => {
        if (cancelled) return;
        if (res?.code !== 0)
          throw new Error(res?.message || "Load tables failed");

        const tables: string[] = (res?.data || [])
          .map((t: any) => (typeof t === "string" ? t : t?.value ?? t?.label))
          .filter(Boolean);

        setTablesState({ loading: false, options: tables }); // ✅ 一次
      })
      .catch((err: any) => {
        if (cancelled) return;
        console.error(err);
        message.error("Load tables failed");
        setTablesState({ loading: false, options: [] }); // ✅ 一次
      });

    return () => {
      cancelled = true;
    };
  }, [selectedSourceDb, dbNameToId]);

  return {
    selectedSourceDb,
    setSelectedSourceDb,
    tableOptions: tablesState.options,
    tableLoading: tablesState.loading,
  };
}

const App: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [activeAgentKey, setActiveAgentKey] = useState("sync_copilot");
  const senderRef = useRef<GetRef<typeof Sender>>(null);

  const lastSourceDbRef = useRef<string>("");

  const { dbOptions, dbNameToId } = useDataSources();
  const { selectedSourceDb, setSelectedSourceDb, tableOptions, tableLoading } =
    useTables(dbNameToId);

  const agentItems: MenuProps["items"] = useMemo(() => {
    return Object.keys(AgentInfo).map((agent) => {
      const { icon, label } = AgentInfo[agent];
      return { key: agent, icon, label };
    });
  }, []);

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
  }, [tableOptions ]);

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
          console.log(sourceDb);
          // 1) 空值直接忽略
          if (!sourceDb || sourceDb === "") return;

          // 2) 关键：同一个值的重复 onChange 直接忽略（包括 Sender 内部回写触发）
          if (sourceDb === lastSourceDbRef.current) return;

          lastSourceDbRef.current = sourceDb;

          console.log("source_db changed =>", sourceDb);

          // 3) 再更新 state
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
