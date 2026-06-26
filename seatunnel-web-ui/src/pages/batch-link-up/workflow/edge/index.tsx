import React, { useState } from 'react';
import { BaseEdge, type EdgeProps, getBezierPath } from 'reactflow';

interface CustomEdgeData {
  executionStatus?: 'running' | 'succeeded' | 'failed' | 'pending';
  onEdgeClick?: (edgeId: string) => void;
  onEdgeMouseEnter?: (edgeId: string) => void;
  onEdgeMouseLeave?: (edgeId: string) => void;
  onOpenInsertMenu?: (
    edgeId: string,
    payload: {
      flowPosition: { x: number; y: number };
      screenPosition: { x: number; y: number };
    },
  ) => void;
}

const CustomEdge: React.FC<EdgeProps> = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  style = {},
  data,
  markerEnd,
  selected,
}) => {
  const [hovered, setHovered] = useState(false);
  const edgeData = data as CustomEdgeData | undefined;

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  });

  // 如果 style 中已经有 stroke 属性（来自 onNodeMouseEnter），就使用它
  // 否则根据状态返回颜色
  const strokeColor =
    style.stroke ||
    (edgeData?.executionStatus === 'running'
      ? '#faad14'
      : edgeData?.executionStatus === 'succeeded'
        ? '#17b26a'
        : edgeData?.executionStatus === 'failed'
          ? '#ff4d4f'
          : edgeData?.executionStatus === 'pending'
            ? '#296dff'
            : '#d0d5dc');

  const handleInsertClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    edgeData?.onOpenInsertMenu?.(id, {
      flowPosition: { x: labelX, y: labelY },
      screenPosition: { x: event.clientX, y: event.clientY },
    });
  };

  return (
    <g
      onMouseEnter={() => {
        setHovered(true);
        edgeData?.onEdgeMouseEnter?.(id);
      }}
      onMouseLeave={() => {
        setHovered(false);
        edgeData?.onEdgeMouseLeave?.(id);
      }}
      onClick={(event) => {
        event.stopPropagation();
        edgeData?.onEdgeClick?.(id);
      }}
      style={{ cursor: 'pointer' }}
    >
      <path
        d={edgePath}
        fill="none"
        stroke="transparent"
        strokeWidth={18}
        className="react-flow__edge-interaction"
      />

      <BaseEdge
        path={edgePath}
        markerEnd={markerEnd}
        style={{
          ...style,
          stroke: hovered || selected ? '#315EFB' : strokeColor,
          strokeWidth: hovered || selected ? 2.2 : style.strokeWidth || 1.5,
        }}
      />

      {hovered && edgeData?.onOpenInsertMenu && (
        <foreignObject
          width={32}
          height={32}
          x={labelX - 16}
          y={labelY - 16}
          requiredExtensions="http://www.w3.org/1999/xhtml"
        >
          <button
            className="nodrag nopan"
            type="button"
            aria-label="在连接线上插入节点"
            onClick={handleInsertClick}
            style={{
              width: 24,
              height: 24,
              margin: 4,
              borderRadius: 999,
              border: '1px solid #C7D7FE',
              background: '#FFFFFF',
              color: '#315EFB',
              boxShadow: '0 8px 20px rgba(49, 94, 251, 0.18)',
              cursor: 'pointer',
              fontSize: 18,
              fontWeight: 600,
              lineHeight: '20px',
              padding: 0,
            }}
          >
            +
          </button>
        </foreignObject>
      )}
    </g>
  );
};

export default CustomEdge;
