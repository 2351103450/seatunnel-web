# 企业版本兼容矩阵

本文件是企业发布的必需记录。上游基线、SeaTunnel、运行时、数据库、Flyway schema 或镜像发生变化时，必须在同一个 PR 更新。

## 当前基线

以下内容是制定本规范时从本地 `main` 和项目 README 观察到的初始基线，正式发布前必须由发布负责人复核并补齐：

| 企业版本 | 企业 Tag | 上游 Commit | SeaTunnel | Java | Node.js | Yarn | 数据库 | Flyway Schema | 镜像 Digest | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `待定` | `待定` | `cad171ee3ccd6f3c05dec53a7e2a14e188c9994b` | `2.3.13` | `21` | `22.22.1` | `1.22.22` | `MySQL 8.0` | `1.0.3` | `待定` | 初始待验证 |

## 维护规则

- 每个生产企业版本单独一行，历史行不得覆盖或删除。
- 上游 Commit 使用完整 SHA；短 SHA 仅可用于展示。
- Java、Node.js、Yarn 和数据库填写实际构建/生产版本，不只填写最低要求。
- SeaTunnel 版本必须与 E2E 实际使用的 Engine 版本一致。
- Flyway Schema 填写发布后 `flyway_schema_history` 的最高成功版本。
- 镜像记录不可变 digest；tag 仅作为人类可读别名。
- 同一个企业版本支持多套组合时，应拆分为多行并标注支持级别。
- 未经矩阵登记和验证的组合视为不受支持。

## 支持级别

| 级别 | 含义 |
| --- | --- |
| `生产支持` | 已通过完整 CI、升级测试、E2E 和生产发布审批 |
| `候选` | 已构建并测试，尚未完成生产审批 |
| `实验` | 仅用于验证，不承诺升级和生产支持 |
| `停止支持` | 不再修复；保留记录用于审计和回滚参考 |

## 发布命名

推荐：

```text
Git tag: enterprise-v1.2.0
Image tag: 1.2.0-st2.3.13
Image reference: registry.example.com/data-platform/seatunnel-web@sha256:<digest>
```

禁止在生产部署中使用：

```text
latest
main
dev
```
