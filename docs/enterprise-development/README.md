# SeaTunnel Web 企业二次开发治理规范

> 状态：生效草案  
> 适用仓库：`liuchang012463/seatunnel-web`  
> 上游仓库：`weifuwan/seatunnel-web`  
> 维护范围：企业功能开发、上游同步、测试、发布与回滚

## 1. 目的与规则级别

本规范用于保证企业版能够持续交付，同时长期吸收上游 `main` 的新功能。所有进入企业仓库的代码和数据库变更都必须遵守本规范。

本文使用以下关键词：

- **必须**：合并前不可豁免，除非完成第 14 节的例外审批。
- **应该**：默认执行；不执行时必须在 PR 中说明理由。
- **可以**：按功能需要选择。

核心原则：

1. 企业 `main` 表示可生产发布的稳定版本，不等同于上游 `main`。
2. 企业能力优先外置或放入独立模块，避免散落修改上游代码。
3. 必须修改上游核心时，改动应小、集中、可测试、可移除，并登记为企业补丁。
4. 每次上游同步都按一次正式软件升级处理，不允许自动进入生产。
5. 数据库、任务定义和密钥兼容性与 Git 冲突同等重要。

## 2. 仓库与 Remote 约定

仓库角色固定如下：

| 名称 | 地址 | 角色 |
| --- | --- | --- |
| `origin` | `https://github.com/liuchang012463/seatunnel-web.git` | 企业 Fork，可推送 |
| `upstream` | `https://github.com/weifuwan/seatunnel-web.git` | 社区上游，只读 |

新克隆推荐使用：

```bash
git clone https://github.com/liuchang012463/seatunnel-web.git
cd seatunnel-web
git remote add upstream https://github.com/weifuwan/seatunnel-web.git
git fetch --all --prune
```

如果本地仓库当前的 `origin` 指向上游，先确认没有依赖旧 remote 名称的脚本，再执行：

```bash
git remote rename origin upstream
git remote add origin https://github.com/liuchang012463/seatunnel-web.git
git fetch --all --prune
```

必须满足：

- 禁止向 `upstream` 推送企业代码。
- 自动化脚本不得通过模糊的默认 remote 推断仓库角色。
- PR、Issue、Release 和企业镜像必须在企业 Fork 中管理。

## 3. 长期分支模型

| 分支 | 用途 | 直接开发 | 保护要求 |
| --- | --- | ---: | --- |
| `upstream/main` | 上游最新代码 | 否 | 只读远程引用 |
| `upstream-sync` | 企业仓库内的纯上游镜像 | 否 | 仅同步机器人可更新 |
| `develop` | 企业日常集成 | 否 | 必须通过 PR 和 CI |
| `feature/*` | 企业功能 | 是 | 从 `develop` 创建 |
| `bugfix/*` | 非生产紧急缺陷 | 是 | 从 `develop` 创建 |
| `upstream/YYYY-MM-DD-*` | 单次上游同步 | 仅解决同步问题 | 合入 `develop` 后删除 |
| `release/x.y.z` | 发布候选与验收修复 | 受控 | 只接受发布相关变更 |
| `hotfix/x.y.z` | 生产紧急修复 | 受控 | 从 `main` 创建，并回合 `develop` |
| `main` | 企业生产稳定版本 | 否 | 必须通过发布 PR |

分支保护必须至少启用：

- `main`、`develop` 禁止直接 push 和强制 push。
- 至少一名非作者审批；安全、认证、权限、密钥和数据库变更至少两名审批。
- 必需状态检查全部通过后才允许合并。
- `main` 仅接受 `release/*`、`hotfix/*` 或明确批准的回滚 PR。
- 同一 PR 不得混合上游同步与无关企业功能。

## 4. 变更分类与隔离顺序

设计企业功能时，必须按以下顺序选择实现位置：

1. **外部系统集成**：SSO、企业门户、工单、CMDB、密钥中心、消息中心、元数据或审计平台优先通过 API、网关或事件集成。
2. **企业独立模块**：需要与本应用同进程部署的功能放入独立 Maven 模块或前端企业目录。
3. **现有 SPI/插件机制**：优先复用 `seatunnel-web-spi`、数据源插件、DAO 插件和告警插件的加载模式。
4. **新增通用扩展点**：上游缺少 hook 时，先增加不包含企业语义的通用 SPI，再由企业模块实现。
5. **核心补丁**：只有前四种方式均不适用时才允许直接修改上游核心实现。

核心补丁必须同时：

- 使用单独的原子提交。
- 在 [UPSTREAM_PATCHES.md](./UPSTREAM_PATCHES.md) 登记。
- 提供删除条件和回归测试。
- 评估是否可向上游提交通用 PR。

禁止在上游页面、Controller 或 Service 中散布企业名称、租户判断或公司特例，例如 `if (company == ...)`。

## 5. 后端开发规则

### 5.1 模块和包名

企业代码应该集中在具有明确边界的模块中，例如：

```text
enterprise-extension-spi
enterprise-auth
enterprise-audit
enterprise-approval
enterprise-notification
enterprise-governance
enterprise-dao
enterprise-bootstrap
```

企业 Java 包统一使用组织自有命名空间，例如 `com.<company>.dataplatform.*`。正式开发前必须确定真实域名并替换占位符；禁止将企业业务继续放入 `org.apache.seatunnel.web.*`。

### 5.2 依赖方向

依赖方向必须保持为：

```text
enterprise implementation
        ↓
enterprise extension SPI / upstream public SPI
        ↓
upstream core modules
```

上游核心模块不得反向依赖具体企业实现。装配关系集中在 `enterprise-bootstrap` 或 Spring 配置类中。

### 5.3 扩展点规则

认证、鉴权、作业发布、作业提交、审计事件和密钥解析等能力应以接口扩展，例如：

```text
AuthenticationProvider
AuthorizationProvider
JobPublishInterceptor
JobSubmitInterceptor
AuditEventPublisher
SecretResolver
```

新增扩展点必须：

- 使用领域中立的名称和参数，不泄露企业实现细节。
- 定义无企业插件时的默认行为。
- 明确多实现的顺序、冲突处理和失败策略。
- 包含接口契约测试和至少一个默认实现测试。
- 兼容现有调用方；破坏性变更必须通过 ADR 和版本升级批准。

## 6. 前端开发规则

企业前端代码统一放在：

```text
seatunnel-web-ui/src/enterprise/
├── api/
├── auth/
├── components/
├── pages/
├── permissions/
├── plugins/
└── routes/
```

必须遵守：

- 企业路由、菜单、按钮和权限通过集中注册函数装配。
- 对 `seatunnel-web-ui/config/routes.ts` 等上游入口的修改只能保留一个稳定接入点。
- 不得复制整个上游页面后长期分叉；应该组合组件或提供扩展槽位。
- API 类型和错误码应统一管理，不得在页面中重复拼接接口路径。
- 所有权限限制都必须有后端校验；前端隐藏按钮不构成安全控制。
- 上游同步后必须回归历史任务的打开、编辑、字段映射和提交行为。

## 7. 数据库与迁移规则

当前项目使用 Flyway，迁移位置为 `classpath:db/migration/{vendor}`。企业迁移应放在 `enterprise-dao/src/main/resources/db/migration/<vendor>/`，并确保该模块进入最终运行时 classpath。

### 7.1 版本与命名

- 上游迁移保留其原始版本和内容，已发布迁移严禁修改。
- 企业迁移版本从 `V1000_...` 开始，例如 `V1000_1__create_ent_audit_log.sql`。
- 企业表统一使用 `ent_` 前缀。
- 每个受支持数据库 vendor 都必须提供等价迁移；暂不支持的 vendor 必须在部署文档和启动检查中明确阻断。
- SQL 文件名、对象名和注释必须表达业务含义，禁止使用 `tmp`、`new_table` 等临时命名。

### 7.2 表设计

企业字段优先放入一对一或一对多扩展表，而不是修改上游表。例如：

```sql
CREATE TABLE ent_job_definition_ext (
    job_definition_id BIGINT PRIMARY KEY,
    department_id BIGINT,
    project_id BIGINT,
    security_level VARCHAR(32),
    approval_policy VARCHAR(64)
);
```

只有查询性能、事务一致性或上游模型约束证明扩展表不可行时，才允许修改上游表，并必须登记企业补丁。

### 7.3 迁移质量门禁

数据库 PR 必须包含：

- 空库全量迁移测试。
- 上一企业生产版本升级测试。
- Flyway `validate` 通过。
- 数据量和锁表影响评估。
- 备份、验证和回滚方案。
- 对历史任务定义、调度和运行记录的兼容性验证。

删除表或列必须采用 Expand–Migrate–Contract，至少跨一个稳定版本完成；生产回滚以应用回退、向后兼容迁移和发布前备份为主，不依赖未经验证的自动 down 脚本。

## 8. 配置、密钥与安全规则

- 企业配置使用独立 profile 或命名空间，例如 `application-enterprise.yml` 和 `enterprise.*`。
- 环境差异通过环境变量或配置中心注入，不得提交生产地址、账户、令牌、证书私钥或密码。
- 数据源凭据必须加密存储或只保存密钥引用；API 响应、日志、异常和审计事件均不得返回明文。
- 密钥读取必须经 `SecretResolver` 或等价集中组件，不得在业务代码中直接访问不同密钥系统。
- 认证、授权、审批、密钥、上传、表达式执行和外部命令相关变更必须执行安全评审。
- 第三方依赖升级必须检查漏洞、许可证和与 Java 21/Node.js/Yarn Classic 的兼容性。

## 9. 提交与 PR 规则

提交信息使用 Conventional Commits：

```text
feat(auth): add enterprise SSO adapter
feat(extension): add job submit interceptor
fix(audit): mask datasource credentials
chore(upstream): merge upstream main at <commit>
docs(governance): update compatibility matrix
```

PR 必须做到：

- 一个 PR 只解决一个主题；格式化、重命名和业务修改不得无关混合。
- 描述变更类型、影响模块、测试证据、数据库影响、配置影响、安全影响和回滚方式。
- 修改上游核心文件时，链接对应的补丁编号。
- 新增功能包含单元测试；跨模块流程包含集成测试；关键用户路径包含 E2E 或可复现验收记录。
- UI 变更附截图；API 变更附请求/响应示例；迁移变更附升级前后验证结果。

功能分支合并 `develop` 时优先 squash；上游同步 PR 必须保留一次明确的 `--no-ff` 合并边界，不做长期 rebase 或历史重写。

## 10. 上游同步流程

### 10.1 周期

- 每周自动检查上游新提交。
- 默认每月完成一次正式同步；变更活跃期可调整为双周。
- 安全漏洞、关键缺陷或引擎兼容问题随时触发紧急同步。

自动化只允许检测、生成报告、创建 Issue/候选 PR 和运行预合并测试，不得直接合入 `develop` 或 `main`。

### 10.2 建立纯上游镜像

首次创建镜像分支时执行：

```bash
git fetch upstream main
git push origin upstream/main:refs/heads/upstream-sync
```

后续确认 `upstream-sync` 不包含任何企业提交后，执行：

```bash
git fetch upstream main
git push origin upstream/main:refs/heads/upstream-sync --force-with-lease
```

### 10.3 创建同步分支

```bash
git fetch --all --prune
git switch develop
git pull --ff-only origin develop
git switch -c upstream/YYYY-MM-DD-short-description
git merge --no-ff upstream/main
```

发生冲突时必须：

1. 先确认冲突属于上游代码、企业扩展点还是企业业务。
2. 不得为“快速通过”删除上游测试、迁移或安全修复。
3. 核对 [UPSTREAM_PATCHES.md](./UPSTREAM_PATCHES.md) 中每个补丁是否仍需要。
4. 上游已提供等价能力时，删除企业补丁和重复实现。
5. 单独提交冲突解决，避免夹带新功能。

同步 PR 必须复制并完成 [UPSTREAM_SYNC_CHECKLIST.md](./UPSTREAM_SYNC_CHECKLIST.md)，记录旧/新上游 commit、提交数量、数据库变化、依赖变化、影响评估、测试结果和回滚方式。

## 11. CI 与验收门禁

所有普通 PR 和上游同步 PR 至少执行：

### 11.1 后端

```bash
./mvnw --batch-mode --no-transfer-progress -T 1C clean verify
```

必须包含编译、单元测试、集成测试、代码风格/静态检查以及适用的数据库迁移测试。

### 11.2 前端

```bash
corepack enable
corepack prepare yarn@1.22.22 --activate
cd seatunnel-web-ui
yarn install --frozen-lockfile
yarn lint
yarn jest --ci
yarn build
```

若测试脚本名称变化，应同步更新 CI 和本文档，不得通过跳过测试解决依赖或环境问题。

### 11.3 企业关键回归

至少覆盖：

- 登录、退出、会话过期和 SSO 回调。
- 用户/角色/项目空间及数据源授权隔离。
- 数据源创建、连通性测试、元数据浏览和凭据脱敏。
- 批任务、流任务、CDC、字段映射和 HOCON 生成。
- 发布审批、提交、停止、重试、日志、指标和调度。
- 历史任务在升级后的打开、编辑和运行。
- 审计事件完整性与敏感信息过滤。
- 从上一生产版本数据库升级并完成冒烟测试。

任一必需门禁失败时禁止通过人工合并绕过。

## 12. 版本、发布与回滚

每个企业版本必须更新 [COMPATIBILITY_MATRIX.md](./COMPATIBILITY_MATRIX.md)，至少记录：

- 企业版本与 Git tag。
- 上游基线 commit。
- SeaTunnel、Java、Node.js、Yarn 和数据库版本。
- Flyway schema 版本和镜像 digest。

发布路径固定为：

```text
develop → release/x.y.z → main → enterprise-vx.y.z
```

生产镜像不得使用 `latest`、`main` 或 `dev`；推荐使用 `x.y.z-st<seatunnel-version>` 并在发布记录中保存 digest。

发布物必须至少包括：

- 应用与前端镜像或安装包。
- 数据库迁移脚本、发布前备份确认和恢复说明。
- 配置模板、升级说明、回滚说明和兼容矩阵。
- 测试报告、SBOM、校验和及第三方许可证清单。

发布后发现问题时：代码优先回退到上一不可变镜像；数据库按已验证的兼容策略处理。禁止在未评估数据损失的情况下直接反向执行 DDL。

## 13. 文档与台账

以下文档是交付物的一部分，必须与代码在同一个 PR 更新：

| 文档 | 触发条件 |
| --- | --- |
| [UPSTREAM_PATCHES.md](./UPSTREAM_PATCHES.md) | 新增、修改或删除核心补丁 |
| [UPSTREAM_SYNC_CHECKLIST.md](./UPSTREAM_SYNC_CHECKLIST.md) | 每次上游同步 |
| [COMPATIBILITY_MATRIX.md](./COMPATIBILITY_MATRIX.md) | 发布、上游基线或运行时版本变化 |
| API/部署/运维文档 | 接口、配置、告警、部署或恢复流程变化 |
| ADR | 架构边界、数据模型、安全模型或扩展机制发生重大变化 |

项目原始技术指标基线保存在 `docs/rocket/`：

- [数据采集引接软件指标](../rocket/数据采集引接软件指标.md)
- [数据资产管理软件指标](../rocket/数据资产管理软件指标.md)
- [功能分解工作簿](../rocket/功能分解.xlsx)

原始指标是需求追踪输入，不得因实现困难直接删改。指标发生变化时，必须保留来源、版本、批准记录，并在需求/设计/测试用例中建立可追踪关系。

## 14. 例外处理

确实无法遵守“必须”规则时，PR 合并前必须记录：

1. 被豁免的规则。
2. 业务和技术原因。
3. 风险、影响范围和监控方式。
4. 临时补偿措施。
5. 责任人、到期日期和清理 Issue。
6. 技术负责人；涉及安全或数据时还需安全/DBA 负责人批准。

永久例外必须通过 ADR 修订本规范，不能依赖口头约定。

## 15. Definition of Done

一个企业功能只有同时满足以下条件才算完成：

- 实现位置符合隔离顺序，没有无必要的上游核心修改。
- 测试、静态检查、构建和迁移验证全部通过。
- 权限和敏感数据由后端强制保护。
- 配置、部署、监控和回滚路径已明确。
- 补丁、兼容矩阵、API 和运维文档已按触发条件更新。
- PR 审批完成且没有未处理的高风险问题。

## 16. 首次落地清单

本规范生效后，应优先完成以下一次性工作：

- [ ] 将企业 Fork 配置为 `origin`，上游配置为 `upstream`。
- [ ] 创建并保护 `upstream-sync`、`develop` 和 `main`。
- [ ] 为 `main`、`develop` 配置必需 CI 状态检查和 CODEOWNERS。
- [ ] 在前端 CI 中补齐 lint 和测试门禁。
- [ ] 确定正式企业 Java 包名、镜像仓库和产品名称。
- [ ] 建立每周上游检查任务和月度同步负责人。
- [ ] 完成首版兼容矩阵、备份恢复演练和关键 E2E 基线。
- [ ] 保留 Apache License 2.0 的 LICENSE、NOTICE 和版权声明，产品名称不得暗示 Apache 官方发行。
