import type { Edge, Node, XYPosition } from 'reactflow';

export interface TransformNodeConfig {
  position: XYPosition;
  nodeType?: string;
  label: string;
  componentType: string;
  iconType?: string;
}

const createTransformData = ({
  nodeType = 'transform',
  label,
  componentType,
  iconType,
}: Omit<TransformNodeConfig, 'position'>) => {
  if (componentType === 'FIELDMAPPER') {
    return {
      label,
      title: label,
      description: '配置字段映射关系',
      nodeType,
      componentType,
      iconType,
      config: {
        mappings: [],
        passThroughUnmapped: true,
      },
      meta: {
        inputSchema: [],
        outputSchema: [],
        schemaStatus: 'idle',
        schemaError: '',
      },
    };
  }

  if (componentType === 'SQL') {
    return {
      label,
      title: label,
      description: '支持自定义查询逻辑',
      nodeType,
      componentType,
      iconType,
      config: {
        sql: '',
      },
      meta: {
        inputSchema: [],
        outputSchema: [],
        schemaStatus: 'idle',
        schemaError: '',
      },
    };
  }

  return {
    label,
    nodeType,
    componentType,
    iconType,
    config: {},
    meta: {
      inputSchema: [],
      outputSchema: [],
      schemaStatus: 'idle',
      schemaError: '',
    },
  };
};

export const createTransformNode = ({
  position,
  nodeType = 'transform',
  label,
  componentType,
  iconType,
}: TransformNodeConfig): Node => {
  const id = `${nodeType}-${Date.now()}-${Math.random()
    .toString(36)
    .slice(2, 8)}`;

  return {
    id,
    type: 'custom',
    position,
    data: createTransformData({
      nodeType,
      label,
      componentType,
      iconType,
    }),
  };
};

export const createWorkflowEdge = (
  source: string,
  target: string,
  data?: Record<string, any>,
): Edge => ({
  id: `${source}-${target}-${Date.now()}-${Math.random()
    .toString(36)
    .slice(2, 8)}`,
  source,
  target,
  type: 'custom',
  data: data || {},
});

export type InsertableTransformNode = Omit<TransformNodeConfig, 'position'> & {
  description: string;
};

export const insertableTransformNodes: InsertableTransformNode[] = [
  {
    nodeType: 'transform',
    componentType: 'FIELDMAPPER',
    iconType: 'braces',
    label: '字段映射',
    description: '配置字段对应关系',
  },
  {
    nodeType: 'transform',
    componentType: 'SQL',
    iconType: 'database',
    label: 'SQL 脚本',
    description: '支持自定义查询',
  },
];
