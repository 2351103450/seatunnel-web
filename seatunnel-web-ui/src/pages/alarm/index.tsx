import ClickSpark from '@/components/ClickSpark';
import { useIntl } from '@umijs/max';
import { Tabs } from 'antd';
import { motion } from 'framer-motion';
import React, { useState } from 'react';
import AlarmPageHeader from './components/AlarmPageHeader';
import ChannelTab from './components/ChannelTab';
import RecordTab from './components/RecordTab';
import RuleTab from './components/RuleTab';
import { PAGE_ANIMATION } from './constants';

const AlarmPage: React.FC = () => {
  const intl = useIntl();
  // 惰性加载：首次切到某 Tab 才渲染其内容，避免首屏一次拉全部数据
  const [visited, setVisited] = useState<Record<string, boolean>>({
    channels: true,
  });
  const [activeKey, setActiveKey] = useState('channels');

  const handleTabChange = (key: string) => {
    setActiveKey(key);
    setVisited((prev) => (prev[key] ? prev : { ...prev, [key]: true }));
  };

  const items = [
    {
      key: 'channels',
      label: intl.formatMessage({
        id: 'pages.alarm.tab.channels',
        defaultMessage: '告警通道',
      }),
      children: visited.channels ? <ChannelTab /> : null,
    },
    {
      key: 'rules',
      label: intl.formatMessage({
        id: 'pages.alarm.tab.rules',
        defaultMessage: '告警规则',
      }),
      children: visited.rules ? <RuleTab /> : null,
    },
    {
      key: 'records',
      label: intl.formatMessage({
        id: 'pages.alarm.tab.records',
        defaultMessage: '告警记录',
      }),
      children: visited.records ? <RecordTab /> : null,
    },
  ];

  return (
    <>
      <ClickSpark
        sparkColor="hsl(231 48% 48%)"
        sparkSize={10}
        sparkRadius={15}
        sparkCount={8}
        duration={400}
        easing="ease-out"
        extraScale={1}
      >
        <div className="alarm-page-container">
          <motion.div
            initial="hidden"
            animate="visible"
            variants={PAGE_ANIMATION.sectionStagger}
          >
            <motion.div variants={PAGE_ANIMATION.fadeUp}>
              <AlarmPageHeader />
            </motion.div>

            <motion.div variants={PAGE_ANIMATION.fadeUp}>
              <div className="rounded-3xl bg-white p-5 shadow-[0_1px_3px_rgba(16,24,40,0.06)]">
                <Tabs
                  activeKey={activeKey}
                  onChange={handleTabChange}
                  items={items}
                  size="large"
                />
              </div>
            </motion.div>
          </motion.div>
        </div>
      </ClickSpark>
    </>
  );
};

export default AlarmPage;
