# MySeek 开发任务清单

> 单页查看开发进度与下一步。详细验收证据见工作区根目录 `IMPLEMENTATION_STATUS.md` 与 `FEATURE_SUMMARY.md`。
> 状态约定：`[x]` 已完成并有验证记录；`[ ]` 未完成；标有「未验证」的项表示代码已落地但缺少实跑证据。

最近更新：2026-11-23

## 进度总览

| 方向 | 状态 | 说明 |
| --- | --- | --- |
| V2 第一～第五阶段 | 已完成 | 知识库页、写作仿写、用户管理、测试/CI、部署与可观测性 |
| V2 后续增强 | 已完成 | RAG 生成回答、写作云同步、Ollama 运行时配置、知识库隔离、会话删除、知识库接入对话/写作、用户工作目录 |
| 一键 Demo 启动脚本与文档 | 代码完成，未提交 | `doc/demo.sh`、`application-demo.yml`、H2/Luban-RDS 依赖、三仓库 README、宣传页 Docs/i18n |
| Demo 环境本地实跑 | 已验证 | `demo` profile 启动成功：H2 文件生成、Luban-RDS 监听自定义端口、`/actuator/health` = UP |
| 完整 RAG 实跑 | 未验证 | Demo 用 Luban-RDS（无 RediSearch），RAG 被禁用；需在 Redis Stack 环境验证 |
| E2E 补跑 | 部分完成 | 供应商 2 例、聊天 2 例已通过；历史会话删除、聊天开关偏好用例未运行 |
| 知识库健壮性增强 | 部分完成 | 异步上传 + 状态轮询 + 失败重试 + 分页修正已落地；删除补偿、同名去重、历史归属待办 |
| AI 任务中心一期 | 已完成 | 新建目标 → 计划确认 → 异步执行 → 步骤进度/日志 → 取消/失败重试；5 个按用户隔离的白名单工具；敏感工具审批 |

## 一、当前任务（按优先级）

### P0 — 交付与验收

- [ ] 提交本轮改动：一键 Demo（`doc/demo.sh`、`application-demo.yml`、`application.yml` 合并重复 `app` 键）、知识库异步化（`V5` 迁移、处理服务、前端轮询/重试）与 AI 任务中心（`V6`/`V7` 迁移、三张表、`/api/tasks/**`、任务运行时与白名单工具、敏感工具审批、前端任务中心页面）。当前均为工作区未提交状态。
- [ ] 在 Redis Stack 环境实跑完整 RAG：Demo 模式用 Luban-RDS 替代 Redis Stack，不具备 `FT.*` 向量命令，知识库问答需在真实 Redis Stack 下回归。
- [ ] Docker Compose 实跑验收（与 Demo 无关，属于生产交付）：构建后端/前端镜像，验证 MariaDB、Redis Stack（RediSearch）、Ollama、后端 8080、Web 3000 的启动顺序与健康检查。
- [ ] 推送后确认三个仓库 GitHub Actions CI 均实际跑通（此前只确认 workflow 文件存在）。
- [ ] 后端重启后回归会话删除接口：旧进程曾返回 `No static resource api/ai/chat/item/...`，新路由已由 `ChatControllerDeleteTest` 验证。

### P1 — 稳定性与验证

- [x] 知识库上传异步化：请求线程只落盘入库，后台线程池解析/向量化；前端轮询 `PENDING/PROCESSING`，失败可重试（最多 3 次），启动后自动恢复未完成任务。证据：`KnowledgeAsyncTest` 3 例、后端全量测试、`pnpm test` 33 例、`tsc --noEmit` 通过。
- [ ] 知识库删除一致性补偿：向量或本地文件删除失败目前仅告警不中断，可能产生孤儿向量；需要补偿或重试。异步化已加「处理中禁止删除」保护，剩余一致性问题仍未解决。
- [ ] 补跑 E2E：`e2e/chatHistoryDelete.spec.ts`、`chat.spec.ts` 中聊天开关偏好相关用例。
- [ ] 验证 Flyway 生产迁移链：`V1`～`V7` 在 `ddl-auto=validate` 下与现有生产库结构一致。

### P2 — 体验与清理

- [ ] 同名重复上传去重，避免重复记录与冗余向量。
- [x] 知识库分页交互修正：上传后仅在非第 0 页时触发 `setPage(0)`，避免重复请求；删除末页最后一项后自动回退页码。证据：`KnowledgeManager.tsx`。
- [ ] 历史数据归属：为存量 `created_by IS NULL` 的文档提供管理员批量分配入口。
- [ ] 梳理 `my-seek/doc`：迁移旧提交遗留的小说/文档内容到 `MYSEEK_WORK_DIR/novels`，**保留** `doc/demo.sh` 与 `doc/push-gitee.ps1`。
- [ ] 处理前端生产构建中 Markdown lazy chunk 仍超过 500 kB 的提示（当前为按需加载，功能不受影响）。
- [x] AI 任务中心 `ask` 级工具审批：写操作与模型标记的步骤进入 `WAIT_APPROVAL`，前端展示工具与参数并支持批准/拒绝；批准后可继续执行，拒绝则终止。证据：`TaskServiceTest` 6 例、前端 `taskService.test.ts`、`demo` profile 健康检查 UP。
- [ ] AI 任务中心二期剩余：子 agent 并行、SSE 事件流、MCP 连接器。
- [ ] AI 任务中心实际联调：在有可用模型的环境下创建任务 → 确认计划 → 执行 → 查看结果；目前仅完成单测与启动验证。

## 二、已完成（归档）

| 阶段 | 关键产出 | 验证 |
| --- | --- | --- |
| Demo 环境与配置修复 | `application-demo.yml`（H2 + 内嵌 Luban-RDS）、`application.yml` 合并重复 `app` 键、`demo.sh` 改为免 Docker 自动处理 DB/Redis 并不再下载 Ollama | 本机 `demo` profile 启动成功，H2 文件生成、Luban-RDS 监听指定端口、`/actuator/health` = UP |
| 知识库上传异步化 | `@Async` 有界线程池、独立处理服务、`V5` 重试字段、`GET /documents/{id}` 与 `POST /documents/{id}/retry`、前端状态轮询与重试按钮、启动任务恢复 | `KnowledgeAsyncTest` 3 例，后端全量测试通过（1 跳过），前端 33 例 + `tsc --noEmit` 通过 |
| AI 任务中心一期 | `V6` 三张表、`/api/tasks/**` 六个接口、`plan → plan_review → run` 状态机、独立线程池、5 个按用户隔离的白名单工具、前端 `/tasks` 页面 | `TaskServiceTest` 4 例、后端全量测试通过（1 跳过）、前端 36 例 + `tsc --noEmit` 通过、`demo` profile 启动健康检查 UP |
| AI 任务中心工具审批 | `V7` 审批字段、`WAIT_APPROVAL` 状态、`POST /api/tasks/{id}/approve`、敏感写工具自动拦截、前端批准/拒绝卡片 | `TaskServiceTest` 6 例、后端全量测试通过（1 跳过）、前端 36 例 + `tsc --noEmit` 通过、`demo` profile 启动健康检查 UP |
| V2 第一阶段 | 文档基线校准、知识库页面、结构化流协议 `meta/content/done/error` | 后端单测、`pnpm typecheck` |
| V2 第二阶段 | 新建写作弹窗、风格描述持久化、仿写生成与设置页风格编辑 | `pnpm typecheck`、`pnpm build` |
| V2 第三阶段 | `/users` 用户管理页、后端管理员校验、角色/用户字段补充 | `UserControllerTest`、前端构建 |
| V2 第四阶段 | Vitest 测试基础设施、三仓库 CI、长文折叠、会话分批、流式渲染合并 | 17 个前端用例、类型检查、构建 |
| V2 第五阶段 | 多阶段 Dockerfile、根 `docker-compose.yml`、生产清单、requestId 日志与探针 | 后端测试全通过；Docker 实跑未做 |
| RAG 生成回答 | `/api/knowledge/query` 改为模型生成，支持当前模型，清理推理标签 | `KnowledgeBaseServiceTest`、`pnpm typecheck` |
| 写作云端持久化 | `writing_novels` + Flyway `V2`、按用户隔离、localStorage 即时保存与增量同步 | 后端 5 例、前端 4 例 |
| AI 接口配置完善 | `V3__ollama_configs.sql`、Ollama 配置 UI 与运行时热更新、编辑接口带 `id` 修复 | `OllamaModelProviderTest`、`OllamaConfigServiceTest` |
| 测试覆盖补全 | `ModelProviderManagerTest` 等后端单测、Playwright E2E 基础设施与 CI 接入 | 后端 42 通过/1 跳过；前端 21 例 + E2E 2 例 |
| 可观测性增强 | 文件日志带 requestId，搜索/对话/异常结构化日志与耗时 | 后端测试全通过 |
| 知识库隔离与检索优化 | `V4__knowledge_documents_isolation.sql`、向量按用户过滤、相似度阈值与上下文预算、单文档漏召回修复 | `KnowledgeBaseIsolationTest` 6 例、`VectorStoreServiceTest` 2 例 |
| 聊天交互健壮性 | 停止保留末帧、新建中止旧流、重复提交防护、消息 id 递增、历史加载防竞态 | 前端 25 例、E2E 聊天 2 例 |
| 历史会话删除 | 单条/全部删除、Redis 记忆清理、前端二次确认与状态同步 | `ChatServiceDeleteTest` 3 例、`RedisChatMemoryTest` 3 例、前端 29 例 |
| 对话与写作接入知识库 | `useKnowledge` 开关与偏好持久化、按用户检索、失败降级、上下文隔离 | 后端/前端单测通过 |
| 用户工作目录 | `MYSEEK_WORK_DIR` 统一小说与上传目录，Docker 挂载 `backend_work` | `NovelStorageServiceTest` |

## 三、验证命令

```powershell
cd d:/OpenCode/seek/my-seek
./gradlew.bat test

cd d:/OpenCode/seek/my-seek-web
pnpm typecheck
pnpm test
pnpm e2e
pnpm build

cd d:/OpenCode/seek/my-seek-page
pnpm build
```

当前基线：后端 50 个用例通过 / 1 个跳过（`MySeekApplicationTests` 需 MariaDB/Redis）；前端 25 例单测 + 4 例 E2E 通过；宣传站构建通过。

## 四、维护约定

1. 新任务写入对应优先级分组，格式：`- [ ] 任务描述 — 验收标准 / 证据路径`。
2. 完成后勾选 `[x]`，并补充验证证据；只有文件落地但没有验证的项保持 `[ ]` 并标注「未验证」。
3. 阶段性完成后把该组条目移动到「已完成（归档）」表，并更新「最近更新」日期。
4. 进度口径以本文件为准；`IMPLEMENTATION_STATUS.md` 保留逐阶段详细记录，`FEATURE_SUMMARY.md` 保留功能说明。
