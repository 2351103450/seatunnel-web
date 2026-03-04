export default {
  'pages.datasource.header.title': '数据源列表',
  'pages.datasource.header.desc': '一个统一的数据源治理系统，用于管理连接、访问权限和安全策略',


  'pages.datasource.common.title': '数据源',


  'pages.datasource.button.add': '新增',


  'pages.datasource.message.connectSuccess': '连接成功',
  'pages.datasource.message.unknownError': '未知错误',
  'pages.datasource.message.idNotExist': 'id不存在',


  'pages.datasource.delete.confirmTitle': '确认要删除吗？',
  'pages.datasource.delete.confirmContentLine1': '确认删除数据源 [{name}] 吗？',
  'pages.datasource.delete.confirmContentLine2':
    '数据源删除后不可恢复，请谨慎操作。',
  'pages.datasource.delete.okText': '删除',

  'pages.datasource.search.type': '数据源类型',
  'pages.datasource.search.name': '数据源名称',
  'pages.datasource.search.env': '环境',
  'pages.datasource.search.button': '搜索',

  // table columns
  'pages.datasource.table.col.index': '序号',
  'pages.datasource.table.col.dbInfo': '数据库信息',
  'pages.datasource.table.col.env': '环境',
  'pages.datasource.table.col.connInfo': '连接信息',
  'pages.datasource.table.col.status': '状态',
  'pages.datasource.table.col.createTime': '创建时间',
  'pages.datasource.table.col.updateTime': '更新时间',
  'pages.datasource.table.col.operate': '操作',

  // connection labels
  'pages.datasource.table.conn.jdbcUrl': 'JdbcUrl:',
  'pages.datasource.table.conn.schema': 'Schema:',

  // actions
  'pages.datasource.table.action.edit': '编辑',
  'pages.datasource.table.action.delete': '删除',
  'pages.datasource.table.action.test': '测试',

  // tooltips
  'pages.datasource.table.action.edit.tooltip': '编辑数据源',
  'pages.datasource.table.action.delete.tooltip': '删除数据源',
  'pages.datasource.table.action.test.tooltip': '连接测试',

  'pages.datasource.bottom.batchTest': '批量测试',
  'pages.datasource.bottom.batchDelete': '批量删除',

  // modal title
  'pages.datasource.modal.title.add': '新增',
  'pages.datasource.modal.title.edit': '编辑',

  // modal buttons
  'pages.datasource.modal.button.lastStep': '上一步',
  'pages.datasource.modal.button.connTest': '连接测试',
  'pages.datasource.modal.button.finish': '完成',
  'pages.datasource.modal.button.cancel': '取消',

  // modal messages
  'pages.datasource.modal.message.success': '成功',
  'pages.datasource.modal.message.fail': '失败',

  // form base fields
  'pages.datasource.form.dsName': '数据源名称',
  'pages.datasource.form.env': '环境',
  'pages.datasource.form.description': '描述',

  // placeholders
  'pages.datasource.form.inputPlaceholder': '请输入...',
  'pages.datasource.form.selectPlaceholder': '请选择...',

  // required messages
  'pages.datasource.form.dsNameRequired': '数据源名称不能为空',
  'pages.datasource.form.envRequired': '环境不能为空',

  // load config
  'pages.datasource.form.loadConfigFail': '加载表单配置失败',

  // custom other kv
  'pages.datasource.form.other.keyPlaceholder': 'key',
  'pages.datasource.form.other.valuePlaceholder': 'value',
  'pages.datasource.form.other.keyRequired': 'key不能为空',
  'pages.datasource.form.other.valueRequired': 'value不能为空',
  'pages.datasource.form.other.addConnSetting': '新增数据库连接配置',

  'pages.datasource.filter.commonDb': '常用数据库：',
  'pages.datasource.filter.all': '全部',
  'pages.datasource.filter.inputPlaceholder': '请输入...',

  'pages.datasync.header.title': '批量数据同步任务',
  'pages.datasync.header.subtitle': '通过全流程引导式、白屏配置，分钟级构建企业级数据同步任务。',

  'pages.datasync.header.source.placeholder': '源端',
  'pages.datasync.header.source.prefix': '来源：',

  'pages.datasync.header.sink.placeholder': '目的端',
  'pages.datasync.header.sink.prefix': '去向：',

  'pages.datasync.header.button.start': '开始',

  // menu
  'pages.job.menu.view': '查看',
  'pages.job.menu.edit': '编辑',
  'pages.job.menu.delete': '删除',

  // table columns
  'pages.job.table.col.name': '名称',
  'pages.job.table.col.syncPlan': '同步计划',
  'pages.job.table.col.status': '状态',
  'pages.job.table.col.execution': '执行情况',
  'pages.job.table.col.schedule': '调度',
  'pages.job.table.col.createTime': '创建时间',
  'pages.job.table.col.operate': '操作',

  // labels inside Name column
  'pages.job.table.label.jobId': '任务ID',
  'pages.job.table.label.jobName': '任务名',

  // batch actions
  'pages.job.batch.start.success': '全部启动成功',
  'pages.job.batch.start.fail': '全部启动失败',
  'pages.job.batch.stop.success': '全部停止成功',
  'pages.job.batch.stop.fail': '全部停止失败',

  'pages.job.execution.runMode': '运行模式：',
  'pages.job.execution.time': '耗时：',
  'pages.job.execution.amount': '数据量：',
  'pages.job.execution.qps': 'QPS：',
  'pages.job.execution.size': '大小：',

  // units
  'pages.job.execution.unit.seconds': '秒',
  'pages.job.execution.unit.rows': '行',
  'pages.job.execution.unit.rowsPerSecond': '行/秒',

  'pages.job.schedule.cron': 'Cron：',
  'pages.job.schedule.status': '状态：',
  'pages.job.schedule.lastRunTime': '上次运行时间：',
  'pages.job.schedule.nextRunTime': '下次运行时间：',
  'pages.job.schedule.last5RunsTitle': '⏰ 最近 5 次运行时间',

  'pages.job.schedule.status.active': '启用',
  'pages.job.schedule.status.inactive': '停用',

  'pages.job.message.unknownError': '未知错误',

  // common
  'pages.common.yes': '是',
  'pages.common.no': '否',
  'pages.common.success': '成功',

  // messages
  'pages.job.message.idNotExist': 'id不存在',
  'pages.job.message.scheduleIdNotExist': '任务调度ID不存在',


  // delete confirm
  'pages.job.action.delete.confirmTitle': '确认删除？',
  'pages.job.action.delete.confirmContent': '确认删除任务 [{name}] 吗？',
  'pages.job.action.delete.okText': '删除',

  // actions: run/stop
  'pages.job.action.run': '运行',
  'pages.job.action.run.title': '运行任务',
  'pages.job.action.run.desc': '确认运行该任务吗？',

  'pages.job.action.stop': '停止',
  'pages.job.action.stop.title': '停止任务',
  'pages.job.action.stop.desc': '确认停止该任务吗？',

  // schedule
  'pages.job.action.schedule.title': '调度任务',
  'pages.job.action.schedule.enable': '启用',
  'pages.job.action.schedule.disable': '停用',

  'pages.job.action.schedule.online.desc': '确认上线该调度任务吗？',
  'pages.job.action.schedule.online.success': '上线成功',

  'pages.job.action.schedule.offline.desc': '确认下线该调度任务吗？',
  'pages.job.action.schedule.offline.success': '下线成功',

  // more
  'pages.job.action.more': '更多',

  // search form labels
  'pages.job.search.jobName': '任务名',
  'pages.job.search.createTime': '创建时间',
  'pages.job.search.jobId': '任务ID',
  'pages.job.search.status': '状态',
  'pages.job.search.source': '源端',
  'pages.job.search.sink': '目的端',
  'pages.job.search.sourceTable': '源表',
  'pages.job.search.sinkTable': '目的表',

  // placeholders
  'pages.job.search.jobName.placeholder': '请输入任务名',
  'pages.job.search.jobId.placeholder': '请输入任务ID',
  'pages.job.search.selectPlaceholder': '请选择...',
  'pages.job.search.fuzzyPlaceholder': '模糊匹配...',

  // buttons
  'pages.job.search.button.search': '搜索',
  'pages.job.search.button.reset': '重置',

  // expand/collapse
  'pages.job.search.expand': '展开',
  'pages.job.search.collapse': '收起',

  // statuses (display only)
  'pages.job.status.running': '运行中',
  'pages.job.status.completed': '已完成',
  'pages.job.status.failed': '失败',
};