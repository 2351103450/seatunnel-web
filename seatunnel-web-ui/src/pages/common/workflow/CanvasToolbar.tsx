import { useEffect } from 'react';
import { Button, Divider, Tooltip } from 'antd';
import { Focus, GitBranch, Redo2, Undo2 } from 'lucide-react';

interface CanvasToolbarProps {
  canUndo: boolean;
  canRedo: boolean;
  onUndo: () => void;
  onRedo: () => void;
  onAutoLayout: () => void;
  onFitView: () => void;
}

const isEditableTarget = (target: EventTarget | null) => {
  if (!(target instanceof HTMLElement)) return false;

  return Boolean(
    target.closest(
      [
        'input',
        'textarea',
        '[contenteditable="true"]',
        '[role="textbox"]',
        '.ant-input',
        '.ant-select-selection-search-input',
        '.cm-editor',
        '.monaco-editor',
      ].join(','),
    ),
  );
};

export default function CanvasToolbar({
  canUndo,
  canRedo,
  onUndo,
  onRedo,
  onAutoLayout,
  onFitView,
}: CanvasToolbarProps) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (isEditableTarget(event.target)) return;

      const isModifierPressed = event.ctrlKey || event.metaKey;
      const key = event.key.toLowerCase();

      if (!isModifierPressed) return;

      if (key === 'z' && event.shiftKey) {
        if (!canRedo) return;

        event.preventDefault();
        onRedo();
        return;
      }

      if (key === 'z') {
        if (!canUndo) return;

        event.preventDefault();
        onUndo();
        return;
      }

      if (key === 'y') {
        if (!canRedo) return;

        event.preventDefault();
        onRedo();
      }
    };

    window.addEventListener('keydown', handleKeyDown);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [canRedo, canUndo, onRedo, onUndo]);

  return (
    <div className="absolute right-4 top-4 z-10 flex items-center gap-1 rounded-full border border-solid border-[#e4e7ec] bg-white px-2 py-1 shadow-[0_8px_24px_rgba(15,23,42,0.08)]">
      <Tooltip title="撤销（Ctrl+Z）">
        <Button
          aria-label="撤销"
          disabled={!canUndo}
          icon={<Undo2 size={16} />}
          onClick={onUndo}
          shape="circle"
          size="small"
          type="text"
        />
      </Tooltip>
      <Tooltip title="重做（Ctrl+Shift+Z / Ctrl+Y）">
        <Button
          aria-label="重做"
          disabled={!canRedo}
          icon={<Redo2 size={16} />}
          onClick={onRedo}
          shape="circle"
          size="small"
          type="text"
        />
      </Tooltip>
      <Divider className="mx-1 h-5 border-[#e4e7ec]" type="vertical" />
      <Tooltip title="自动布局">
        <Button
          aria-label="自动布局"
          icon={<GitBranch size={16} />}
          onClick={onAutoLayout}
          shape="circle"
          size="small"
          type="text"
        />
      </Tooltip>
      <Tooltip title="适应画布">
        <Button
          aria-label="适应画布"
          icon={<Focus size={16} />}
          onClick={onFitView}
          shape="circle"
          size="small"
          type="text"
        />
      </Tooltip>
    </div>
  );
}
