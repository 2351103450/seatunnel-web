# 上游同步检查单与报告模板

每次从 `weifuwan/seatunnel-web:main` 同步到企业 `develop` 时，复制本模板到同步 PR 描述或 `docs/enterprise-development/sync-reports/YYYY-MM-DD.md`，逐项完成。未完成项必须说明风险和后续 Issue。

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| 同步分支 | `upstream/YYYY-MM-DD-short-description` |
| 负责人 | `@owner` |
| 旧上游基线 | `<old-upstream-commit>` |
| 新上游基线 | `<new-upstream-commit>` |
| 企业目标分支 | `develop` |
| 上游提交数量 | `<count>` |
| 同步 PR | `<url>` |
| 关联发布 | `enterprise-vX.Y.Z` / `待定` |

可使用以下命令生成变更概览：

```bash
git log --oneline <old-upstream-commit>..<new-upstream-commit>
git diff --stat <old-upstream-commit>..<new-upstream-commit>
git diff --name-status <old-upstream-commit>..<new-upstream-commit>
```

## 2. 同步前

- [ ] 已执行 `git fetch --all --prune`。
- [ ] `upstream/main` 指向预期的新基线。
- [ ] `upstream-sync` 是纯上游镜像，不含企业提交。
- [ ] `develop` 已拉取最新企业代码，工作区无意外改动。
- [ ] 已创建独立 `upstream/YYYY-MM-DD-*` 分支。
- [ ] 已备份或可恢复用于升级测试的上一生产版本数据库。
- [ ] 已冻结本次同步范围，同步 PR 不夹带新企业功能。

## 3. 上游变化分类

填写主要变化和企业影响：

| 类别 | 上游变化摘要 | 企业影响 | 风险 | 负责人/处理 |
| --- | --- | --- | --- | --- |
| 后端 API/Core |  | 无/低/中/高 |  |  |
| SPI/插件 |  | 无/低/中/高 |  |  |
| 前端页面/路由 |  | 无/低/中/高 |  |  |
| 数据库/Flyway |  | 无/低/中/高 |  |  |
| 配置/部署 |  | 无/低/中/高 |  |  |
| 依赖/运行时 |  | 无/低/中/高 |  |  |
| 安全修复 |  | 无/低/中/高 |  |  |
| SeaTunnel/Connector |  | 无/低/中/高 |  |  |

## 4. 冲突与补丁处理

- [ ] 所有 Git 冲突均已列出并说明取舍。
- [ ] 未删除或跳过上游测试、安全修复和数据库迁移。
- [ ] 已逐项核对 [UPSTREAM_PATCHES.md](./UPSTREAM_PATCHES.md)。
- [ ] 上游已有等价能力的企业补丁已删除或有明确删除 Issue。
- [ ] 新增的核心修改已登记补丁编号。
- [ ] 冲突解决使用独立提交，便于审计。

冲突记录：

| 文件/模块 | 冲突原因 | 处理方式 | 关联补丁/测试 |
| --- | --- | --- | --- |
|  |  |  |  |

## 5. 数据库与兼容性

- [ ] 已检查所有新增、删除和修改的 Flyway 脚本。
- [ ] 上游已发布迁移脚本未被改写。
- [ ] 空库迁移和上一生产版本升级均通过。
- [ ] Flyway `validate` 通过，无版本冲突或 checksum 异常。
- [ ] 历史任务、调度、运行记录和凭据数据可正确读取。
- [ ] 已评估大表 DDL、锁表时长、磁盘增长和回滚方式。
- [ ] 已更新兼容矩阵中的 schema 版本和数据库版本。

## 6. 构建与自动化测试

| 门禁 | 结果 | 证据链接/备注 |
| --- | --- | --- |
| 后端 `clean verify` | 通过/失败/不适用 |  |
| 前端 lint | 通过/失败/不适用 |  |
| 前端 Jest | 通过/失败/不适用 |  |
| 前端 build | 通过/失败/不适用 |  |
| 空库迁移 | 通过/失败/不适用 |  |
| 旧版本升级 | 通过/失败/不适用 |  |
| 依赖漏洞扫描 | 通过/失败/不适用 |  |
| 镜像/SBOM 构建 | 通过/失败/不适用 |  |

## 7. 关键业务验收

- [ ] 登录、退出、SSO 和权限隔离。
- [ ] 数据源创建、连接测试、元数据和凭据脱敏。
- [ ] 批任务创建、字段映射、HOCON、提交、停止和日志。
- [ ] 流任务/CDC 创建、运行、停止和指标。
- [ ] 调度、失败重试、告警和审计事件。
- [ ] 发布审批和生产权限控制。
- [ ] 历史任务打开、编辑、发布和运行。
- [ ] Nginx、Web API、WebSocket、MySQL 与 SeaTunnel Engine 完整链路。

失败或豁免记录：

| 场景 | 结果 | 原因 | 风险/补偿措施 | Issue |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

## 8. 发布与回滚结论

- 同步结论：`可合入 develop` / `整改后合入` / `拒绝本次同步`
- 建议企业版本：`X.Y.Z`
- 数据库备份/恢复位置：`<受控记录引用，不填写密钥>`
- 应用回滚目标：`<previous immutable image/tag/digest>`
- 数据库回滚策略：`<forward fix / restore / compatibility mode>`
- 未解决风险：`<summary>`
- 后续 Issue：`<links>`

## 9. 审批

| 角色 | 审批人 | 结论 | 日期 |
| --- | --- | --- | --- |
| 同步负责人 |  |  |  |
| 企业功能负责人 |  |  |  |
| 测试负责人 |  |  |  |
| DBA（有数据库变化时） |  |  |  |
| 安全负责人（有安全变化时） |  |  |  |
| 发布负责人 |  |  |  |
