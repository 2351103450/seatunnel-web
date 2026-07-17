# 企业上游补丁清单

本文件记录企业 Fork 对上游核心代码的必要修改。新增、修改或删除核心补丁时，必须在同一个 PR 更新本文件。

## 状态定义

| 状态 | 含义 |
| --- | --- |
| `active` | 企业当前仍依赖该补丁 |
| `upstream-submitted` | 已提交上游，尚未进入上游 `main` |
| `upstream-merged` | 已进入上游，等待下一次同步删除企业实现 |
| `retired` | 企业补丁已删除；记录保留用于审计 |

## 当前补丁

当前无已登记补丁。

新增补丁后，使用下表作为摘要索引：

| ID | 状态 | 类别 | 摘要 | 主要文件 | 上游 Issue/PR | 删除条件 | Owner | 最近核对 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `PATCH-000` | `active` | `extension` | 示例：增加通用作业提交拦截点 | `path/to/File.java` | `N/A` | 上游提供等价扩展点 | `@owner` | `YYYY-MM-DD` |

## 补丁详情模板

复制以下章节并替换占位内容。补丁编号单调递增，不重复使用。

```markdown
## PATCH-NNN：补丁标题

- 状态：`active`
- 类别：`extension` / `security` / `compatibility` / `bugfix`
- Owner：`@owner`
- 引入版本：`enterprise-vX.Y.Z`
- 最近核对：`YYYY-MM-DD`
- 上游 Issue/PR：`URL` 或 `N/A`

### 背景

说明上游当前缺少什么能力，以及为何外置、独立模块或现有 SPI 无法解决。

### 修改范围

- `path/to/File.java`
- `path/to/test/FileTest.java`

### 企业依赖

列出依赖该补丁的企业模块、配置、表或用户流程。

### 风险与测试

列出上游同步冲突风险、默认行为、回归测试和失败处理。

### 删除条件

说明哪个上游版本/commit 或替代方案出现后可删除，并描述迁移步骤。
```

## 每次上游同步必须核对

- [ ] 所有 `active` 补丁仍有必要。
- [ ] 上游没有出现等价或冲突实现。
- [ ] `upstream-submitted` 补丁的 PR 状态已更新。
- [ ] `upstream-merged` 补丁已安排删除，不保留双重实现。
- [ ] 补丁涉及的回归测试仍有效并已运行。
- [ ] 文件路径、Owner、删除条件和最近核对日期准确。
