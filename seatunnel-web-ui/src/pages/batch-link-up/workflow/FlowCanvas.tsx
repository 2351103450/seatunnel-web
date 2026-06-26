import { Braces, Database } from 'lucide-react';
import { Dropdown } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import ReactFlow, {
  Background,
  MiniMap,
  SelectionMode,
  type Edge,
  type Node,
} from 'reactflow';
import 'reactflow/dist/style.css';

import CanvasToolbar from '../../common/workflow/CanvasToolbar';
import {
  type InsertableTransformNode,
  insertableTransformNodes,
} from '../../common/workflow/graph';
import { ControlMode } from './config';
import CustomEdge from './edge';
import useFlowBuilder from './hooks/useFlowBuilder';
import useNodePlacement from './hooks/useNodePlacement';
import CustomNode from './nodes';
import WorkflowPanel from './panel';

const nodeTypesConfig = {
  custom: CustomNode,
};

const edgeTypes = {
  custom: CustomEdge,
};

const MIN_ZOOM = 0.25;
const MAX_ZOOM = 1;

const insertNodeIconMap: Record<string, React.ReactNode> = {
  FIELDMAPPER: <Braces size={15} />,
  SQL: <Database size={15} />,
};

interface EdgeInsertMenuState {
  edgeId: string;
  flowPosition: { x: number; y: number };
  screenPosition: { x: number; y: number };
}

interface FlowCanvasProps {
  form: any;
  params: any;
  goBack: () => void;
  sourceType?: any;
  targetType?: any;
  onWorkflowChange?: (value: { nodes: any[]; edges: any[] }) => void;
  scheduleConfig?: any;
}

function buildInitialGraph(
  params?: any,
  sourceType?: any,
  targetType?: any,
): {
  nodes: Node[];
  edges: Edge[];
} {
  if (params?.workflow?.nodes?.length) {
    return {
      nodes: params.workflow.nodes || [],
      edges: params.workflow.edges || [],
    };
  }

  const timestamp = Date.now();
  const sourceId = `source-${timestamp}`;
  const sinkId = `sink-${timestamp}`;

  const sourceDbType = sourceType?.dbType || 'MYSQL';
  const targetDbType = targetType?.dbType || 'MYSQL';

  const sourceTitle =
    sourceType?.dbType ||
    sourceType?.pluginName ||
    sourceType?.connectorType ||
    '输入端';

  const sinkTitle =
    targetType?.dbType ||
    targetType?.pluginName ||
    targetType?.connectorType ||
    '输出端';

  const nodes: Node[] = [
    {
      id: sourceId,
      type: 'custom',
      position: { x: 100, y: 180 },
      data: {
        nodeType: 'source',
        title: sourceTitle,
        description: '读取源端数据',
        dbType: sourceDbType,
        connectorType: sourceType?.connectorType,
        pluginName: sourceType?.pluginName,
        config: {
          dataSourceId: params?.sourceDataSourceId || '',
          dbType: sourceType?.dbType,
          connectorType: sourceType?.connectorType,
          pluginName: sourceType?.pluginName,
          pluginOutput: sourceId,
          readMode: 'table',
          table: undefined,
          sql: '',
          extraParams: [],
        },
        meta: {
          outputSchema: [],
          schemaStatus: 'idle',
          schemaError: '',
        },
      },
    },
    {
      id: sinkId,
      type: 'custom',
      position: { x: 460, y: 180 },
      data: {
        nodeType: 'sink',
        title: sinkTitle,
        description: '写入目标端数据',
        dbType: targetDbType,
        connectorType: targetType?.connectorType,
        pluginName: targetType?.pluginName,
        config: {
          dataSourceId: params?.targetDataSourceId || '',
          autoCreateTable: false,
          targetMode: 'table',
          table: undefined,
          targetTableName: '',
          sql: '',
          writeMode: 'append',
          primaryKey: '',
          batchSize: '',
          pluginInput: sinkId,
          extraParams: [],
        },
      },
    },
  ];

  const edges: Edge[] = [
    {
      id: `${sourceId}-${sinkId}`,
      source: sourceId,
      target: sinkId,
      type: 'custom',
      data: {},
    },
  ];

  return { nodes, edges };
}

export default function FlowCanvas({
  form,
  params,
  goBack: _goBack,
  sourceType,
  targetType,
  onWorkflowChange,
  scheduleConfig,
}: FlowCanvasProps) {
  const flow = useFlowBuilder({ form, params });
  const placement = useNodePlacement({
    setNodes: flow.setNodes,
    setControlMode: flow.setControlMode,
  });
  const initializedRef = useRef(false);
  const [edgeInsertMenu, setEdgeInsertMenu] =
    useState<EdgeInsertMenuState | null>(null);

  const closeEdgeInsertMenu = useCallback(() => {
    setEdgeInsertMenu(null);
  }, []);

  const openEdgeInsertMenu = useCallback(
    (
      edgeId: string,
      payload: {
        flowPosition: { x: number; y: number };
        screenPosition: { x: number; y: number };
      },
    ) => {
      flow.selectEdge(edgeId);
      setEdgeInsertMenu({
        edgeId,
        flowPosition: payload.flowPosition,
        screenPosition: payload.screenPosition,
      });
    },
    [flow.selectEdge],
  );

  const handleInsertNodeFromMenu = useCallback(
    (nodeConfig: InsertableTransformNode) => {
      if (!edgeInsertMenu) return;

      flow.insertNodeOnEdge(
        edgeInsertMenu.edgeId,
        edgeInsertMenu.flowPosition,
        nodeConfig,
      );
      closeEdgeInsertMenu();
    },
    [closeEdgeInsertMenu, edgeInsertMenu, flow.insertNodeOnEdge],
  );

  const edgeInsertMenuItems = useMemo(
    () =>
      insertableTransformNodes.map((nodeConfig) => ({
        key: nodeConfig.componentType,
        label: (
          <div className="flex min-w-[180px] items-center gap-3 py-1">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
              {insertNodeIconMap[nodeConfig.componentType]}
            </div>
            <div className="min-w-0">
              <div className="text-[13px] font-semibold leading-[18px] text-slate-800">
                {nodeConfig.label}
              </div>
              <div className="text-[12px] leading-[16px] text-slate-500">
                {nodeConfig.description}
              </div>
            </div>
          </div>
        ),
        onClick: () => handleInsertNodeFromMenu(nodeConfig),
      })),
    [handleInsertNodeFromMenu],
  );

  const interactiveEdges = useMemo(
    () =>
      flow.edges.map((edge) => ({
        ...edge,
        type: edge.type || 'custom',
        selected: edge.id === flow.selectedEdgeId,
        data: {
          ...(edge.data || {}),
          onEdgeClick: flow.onEdgeClick,
          onOpenInsertMenu: openEdgeInsertMenu,
        },
      })),
    [flow.edges, flow.onEdgeClick, openEdgeInsertMenu],
  );

  useEffect(() => {
    onWorkflowChange?.({
      nodes: flow.nodes,
      edges: flow.edges,
    });
  }, [flow.nodes, flow.edges, onWorkflowChange]);

  useEffect(() => {
    if (!params || initializedRef.current) return;

    const hasNodes = Array.isArray(flow.nodes) && flow.nodes.length > 0;
    if (hasNodes) {
      initializedRef.current = true;
      return;
    }

    const { nodes, edges } = buildInitialGraph(params, sourceType, targetType);

    flow.setNodes(nodes);
    flow.setEdges(edges);
    initializedRef.current = true;
  }, [
    params,
    sourceType,
    targetType,
    flow.nodes,
    flow.setNodes,
    flow.setEdges,
  ]);

  const onDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    closeEdgeInsertMenu();
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  };

  const onDrop = (event: React.DragEvent<HTMLDivElement>) => {
    closeEdgeInsertMenu();
    event.preventDefault();

    const raw = event.dataTransfer.getData('application/reactflow');
    if (!raw) return;

    const data = JSON.parse(raw);

    const bounds = placement.reactFlowWrapper.current?.getBoundingClientRect();
    if (!bounds) return;

    const position = flow.screenToFlowPosition({
      x: event.clientX - bounds.left,
      y: event.clientY - bounds.top,
    });

    flow.addNode({
      position,
      nodeType: data.nodeType,
      componentType: data.componentType,
      iconType: data.iconType,
      label: data.label,
    });
  };

  return (
    <div
      className="relative h-full w-full min-w-[960px]"
      style={{
        height: '100%',
        width: '100%',
        cursor: flow.controlMode === ControlMode.Hand ? 'grab' : 'default',
      }}
      ref={placement.reactFlowWrapper}
      onDragOver={onDragOver}
      onDrop={onDrop}
    >
      <CanvasToolbar
        canRedo={flow.canRedo}
        canUndo={flow.canUndo}
        onAutoLayout={flow.autoLayout}
        onFitView={flow.fitWorkflowView}
        onRedo={flow.redo}
        onUndo={flow.undo}
      />

      <ReactFlow
        nodes={flow.nodes}
        edges={interactiveEdges}
        nodeTypes={nodeTypesConfig}
        edgeTypes={edgeTypes}
        onNodesChange={flow.onNodesChange}
        onEdgesChange={flow.onEdgesChange}
        onConnect={flow.onConnect}
        onNodeClick={flow.onNodeClick}
        onEdgeClick={flow.onEdgeClick}
        onNodeContextMenu={flow.onNodeContextMenu}
        onPaneClick={flow.onPaneClick}
        onSelectionChange={flow.onSelectionChange}
        onSelectionContextMenu={flow.onSelectionContextMenu}
        onNodeMouseEnter={flow.onNodeMouseEnter}
        onNodeMouseLeave={flow.onNodeMouseLeave}
        onPaneContextMenu={flow.onPaneContextMenu}
        isValidConnection={flow.isValidConnection}
        selectionMode={SelectionMode.Partial}
        multiSelectionKeyCode={null}
        deleteKeyCode={null}
        minZoom={MIN_ZOOM}
        maxZoom={MAX_ZOOM}
        nodesDraggable={
          !flow.nodesReadOnly && flow.interactionMode === ControlMode.Pointer
        }
        nodesConnectable={!flow.nodesReadOnly}
        nodesFocusable={!flow.nodesReadOnly}
        edgesFocusable={!flow.nodesReadOnly}
        panOnDrag={flow.controlMode === ControlMode.Hand}
        zoomOnPinch={!flow.workflowReadOnly}
        zoomOnScroll={!flow.workflowReadOnly}
        zoomOnDoubleClick={!flow.workflowReadOnly}
        selectionOnDrag={
          flow.interactionMode === ControlMode.Pointer && !flow.workflowReadOnly
        }
        fitView
        fitViewOptions={{
          padding: 0.2,
          minZoom: 0.25,
          maxZoom: 0.75,
        }}
        className={`reactflow-wrapper ${
          flow.controlMode === ControlMode.Hand ? 'hand-mode' : 'pointer-mode'
        }`}
      >
        <Background gap={[14, 14]} size={2} color="#8585ad26" />

        <MiniMap
          position="bottom-left"
          style={{ width: 102, height: 72 }}
          maskColor="#E9EBF0"
        />
      </ReactFlow>

      <Dropdown
        menu={{ items: edgeInsertMenuItems }}
        open={!!edgeInsertMenu}
        onOpenChange={(open) => {
          if (!open) closeEdgeInsertMenu();
        }}
        trigger={['click']}
      >
        <div
          style={{
            position: 'fixed',
            left: edgeInsertMenu?.screenPosition.x || 0,
            top: edgeInsertMenu?.screenPosition.y || 0,
            width: 1,
            height: 1,
            pointerEvents: 'none',
          }}
        />
      </Dropdown>

      <Dropdown
        overlay={flow.renderContextMenu()}
        open={flow.menuVisible}
        onOpenChange={flow.closeContextMenu}
        trigger={['contextMenu']}
      >
        <div
          style={{
            position: 'fixed',
            left: flow.menuPosition.x,
            top: flow.menuPosition.y,
            width: '1px',
            height: '1px',
          }}
        />
      </Dropdown>

      {flow.drawerVisible && (
        <WorkflowPanel
          selectedNode={flow.selectedNode}
          onClose={flow.onCloseDrawer}
          onNodeDataChange={flow.handleNodeDataChange}
          getDirectUpstreamSchema={flow.getDirectUpstreamSchema}
          getFieldMapperLinkedNodeIds={flow.getFieldMapperLinkedNodeIds}
          refreshNodeSchema={flow.refreshNodeSchema}
          refreshDownstreamSchemas={flow.refreshDownstreamSchemas}
          syncTransformPluginConfig={flow.syncTransformPluginConfig}
          scheduleConfig={scheduleConfig}
        />
      )}
    </div>
  );
}
