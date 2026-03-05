import {
  CheckCircleFilled,
  CloseCircleOutlined,
  SyncOutlined,
} from "@ant-design/icons";
import { Alert, List, message, Select, Typography } from "antd";
import React, { useEffect, useMemo, useState } from "react";
import { useIntl } from "@umijs/max";
import { HistoryItem } from "./type";
import { seatunnelJobInstanceApi } from "./api";

interface TaskHistoryPanelProps {
  selectedItem: any;
  statusFilter: string; // 'all' | 'FAILED' | 'FINISHED' | 'RUNNING' | 'PENDING' ...
  onItemSelect: (id: number) => void;
  onStatusFilterChange: (status: string) => void;
  instanceItem: any;
  setInstanceItem: (item: any) => void;
}

const TaskHistoryPanel: React.FC<TaskHistoryPanelProps> = ({
  selectedItem,
  statusFilter,
  onItemSelect,
  onStatusFilterChange,
  instanceItem,
  setInstanceItem,
}) => {
  const intl = useIntl();
  const [historyItems, setHistoryItems] = useState<HistoryItem[]>([]);

  useEffect(() => {
    if (selectedItem?.id) {
      seatunnelJobInstanceApi
        .page({
          jobDefinitionId: selectedItem?.id,
        })
        .then((data) => {
          if (data?.code === 0) {
            setHistoryItems(data?.data?.bizData || []);
          } else {
            message.error(data?.message);
          }
        });
    }
  }, [selectedItem]);

  const filteredItems = useMemo(() => {
    if (statusFilter === "all") return historyItems;
    return historyItems.filter((item) => item.jobStatus === statusFilter);
  }, [historyItems, statusFilter]);

  const getBadgeStatus = (status: HistoryItem["jobStatus"]) => {
    switch (status) {
      case "FAILED":
        return <CloseCircleOutlined style={{ color: "#ff4d4f" }} />;
      case "FINISHED":
        return <CheckCircleFilled style={{ color: "#52c41a" }} />;
      case "RUNNING":
        return <SyncOutlined style={{ color: "#1677ff" }} spin />;
      default:
        return null;
    }
  };

  const statusOptions = [
    {
      value: "all",
      label: intl.formatMessage({
        id: "pages.job.history.status.all",
        defaultMessage: "All Status",
      }),
    },
    {
      value: "FINISHED",
      label: intl.formatMessage({
        id: "pages.job.history.status.finished",
        defaultMessage: "Success",
      }),
    },
    {
      value: "FAILED",
      label: intl.formatMessage({
        id: "pages.job.history.status.failed",
        defaultMessage: "Failed",
      }),
    },
    {
      value: "RUNNING",
      label: intl.formatMessage({
        id: "pages.job.history.status.running",
        defaultMessage: "Running",
      }),
    },
    {
      value: "PENDING",
      label: intl.formatMessage({
        id: "pages.job.history.status.pending",
        defaultMessage: "Pending",
      }),
    },
  ];

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        height: "100%",
        borderRadius: 4,
        borderRight: "1px solid #f0f0f0",
      }}
    >
      <div style={{ borderBottom: "1px solid #f0f0f0" }}>
        <Alert
          message={intl.formatMessage({
            id: "pages.job.history.tip.last3days",
            defaultMessage: "Only show your run history from the past three days",
          })}
          type="info"
          showIcon
          style={{
            fontSize: 12,
            margin: "4px 6px",
            padding: "1px 8px",
            borderRadius: 6,
          }}
        />
        <div style={{ padding: 6 }}>
          <Select
            style={{ width: "100%" }}
            value={statusFilter}
            size="small"
            onChange={onStatusFilterChange}
            allowClear
            placeholder={intl.formatMessage({
              id: "pages.job.history.status.placeholder",
              defaultMessage: "Filter by status",
            })}
            options={statusOptions}
          />
        </div>
      </div>

      <div style={{ flex: 1, overflow: "auto" }}>
        <List
          dataSource={filteredItems}
          renderItem={(item) => (
            <List.Item
              onClick={() => {
                setInstanceItem(item);
                onItemSelect?.(item.id);
              }}
              style={{
                cursor: "pointer",
                borderRadius: 0,
                padding: "4px 6px",
                backgroundColor:
                  instanceItem?.id === item.id ? "#f5f5f5" : "transparent",
                borderBottom: "1px solid #f0f0f0",
              }}
            >
              <List.Item.Meta
                avatar={getBadgeStatus(item.jobStatus)}
                title={
                  <Typography.Text strong>
                    {item.jobName || "-"}
                  </Typography.Text>
                }
                description={
                  <div style={{ fontSize: 12 }}>{item.startTime || "-"}</div>
                }
              />
            </List.Item>
          )}
        />
      </div>
    </div>
  );
};

export default TaskHistoryPanel;