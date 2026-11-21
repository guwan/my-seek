# MySeek

[English](README.md) | [简体中文](README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.9-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.2-6DB33F)](https://spring.io/projects/spring-ai)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/guwan/my-seek?style=social)](https://github.com/guwan/my-seek)

**MySeek** 是一套面向所有人的私有化 AI 对话工作台后端。它把本地模型、云端大模型、私有知识库与实时联网搜索整合到统一的 API 之下，提供开箱即用的鉴权、会话隔离和可视化模型供应商管理。

> Web 工作台：[my-seek-web](https://github.com/guwan/my-seek-web) · 官方宣传站：[my-seek-page](https://github.com/guwan/my-seek-page)

---

## 核心能力

- **多模型统一接入**：本地 Ollama（DeepSeek 等）、豆包、千问，以及任意 OpenAI 兼容接口，运行时按请求动态切换。
- **可视化供应商管理**：新增 / 编辑 / 启停第三方 AI 接口，API Key 加密入库并支持连通性测试，改完即刻生效，无需改配置重启。
- **联网搜索增强**：对话可按需开启实时搜索，Tavily（需 Key）优先、Wikipedia 免 Key 自动降级，检索结果注入上下文并把来源链接回传前端。
- **RAG 知识库**：文档上传自动解析与向量化，基于 Redis Vector Store 做检索增强问答，支持分页管理与按 `document_id` 删除向量。
- **安全与隔离**：JWT 鉴权 + 图形验证码，聊天记忆与会话列表按用户隔离；敏感配置一律通过环境变量注入。
- **流式对话**：SSE 逐字输出，会话标题自动生成，支持按时间分组与推理过程展示。
- **工程化**：虚拟线程提升 IO 并发、优雅停机、统一全局异常处理、无需数据库 / Redis 即可运行的单元测试。

## 项目组成

| 仓库 | 说明 | 主要技术 |
| --- | --- | --- |
| [guwan/my-seek](https://github.com/guwan/my-seek) | 后端服务：鉴权、对话、知识库、联网搜索、供应商管理 | Java 25 / Spring Boot 4 / Spring AI 2 |
| [guwan/my-seek-web](https://github.com/guwan/my-seek-web) | Web 对话工作台：多模型聊天、会话管理、写作助手 | React 19 / React Router 7 / Redux Toolkit |
| [guwan/my-seek-page](https://github.com/guwan/my-seek-page) | 官方宣传站：产品介绍、特性、文档入口 | Vite 6 / React 19 / Tailwind CSS 4 |

## 界面预览

<p align="center">
  <img src="https://raw.githubusercontent.com/guwan/my-seek-web/main/assets/image.png" alt="MySeek Web 聊天界面" width="920" />
</p>

---

## 技术栈

| 层 | 技术 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot | 4.0.9 |
| AI 编排 | Spring AI | 2.0.2 (GA) |
| ORM | Spring Data JPA / Hibernate | 随 Boot 4.0.9 |
| 序列化 | Jackson | 3.x（Spring Boot 4 默认） |
| 安全 | Spring Security + JJWT | 7.x / 0.13.0 |
| 向量存储 | Redis Vector Store（RediSearch） | 随 Spring AI |
| 构建 | Gradle | 9.9.1（wrapper 已内置） |
| JDK | Java | 25（toolchain 自动解析） |
| 前端 | React + React Router | 19.3.1 / 7.18.5 |
| 前端构建 | Vite + Tailwind CSS | 7.3.8 / 4.3.5 |
| 包管理 | pnpm | 11.x |

## 环境依赖

- JDK 17+（项目 toolchain 固定为 25，Gradle 会自动使用本机已安装的 JDK 25）
- Node.js 24+（pnpm）
- MariaDB / MySQL 8+
- Redis 6+
- （可选）Ollama 本地模型服务

> **启动顺序**：MariaDB → Redis → Ollama → 应用。
> Redis 未启动时应用仍可启动（开发环境不在启动期连接 Redis），但验证码、聊天记忆、知识库等功能会报错。
>
> **知识库（RAG）额外要求**：
> 1. Redis 必须带 RediSearch 模块（Redis Stack），普通 Redis 执行 `FT.*` 会失败；
> 2. 本地 Ollama 需安装嵌入模型：`ollama pull nomic-embed-text`（可用 `OLLAMA_EMBEDDING_MODEL` 覆盖）。

## 关键环境变量

所有敏感配置均通过环境变量注入，不硬编码在配置文件里。

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `JWT_SECRET_KEY` | JWT 签名密钥，至少 32 字节；**生产环境未配置时启动即失败**，开发环境自动生成临时密钥 | 无（prod 必填） |
| `AI_PROVIDER_SECRET` | 加密保存供应商 API Key 的密钥；**生产环境务必覆盖** | 仅开发默认值 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 数据库连接 | `jdbc:mariadb://localhost:3306/seek` / `root` / 空 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 | `localhost` / `6379` / 空 |
| `DOUBAO_API_KEY` / `DOUBAO_BASE_URL` | 豆包模型 | 空 / 火山方舟地址 |
| `QWEN_API_KEY` / `QWEN_BASE_URL` | 千问模型 | 空 / DashScope 兼容地址 |
| `OLLAMA_BASE_URL` / `OLLAMA_CHAT_MODEL` | 本地模型 | `http://localhost:11434` / `deepseek-r1:8b` |
| `WEB_SEARCH_PROVIDER` / `TAVILY_API_KEY` / `TAVILY_BASE_URL` | 联网搜索：provider 取 `tavily` 或 `wikipedia`，Key 为空自动降级 | `tavily` / 空 / Tavily 地址 |
| `WEB_SEARCH_MAX_RESULTS` | 单次联网搜索返回条数 | `5` |
| `MAIL_HOST` / `MAIL_ACCOUNT` / `MAIL_TOKEN` | 邮件服务 | 空 |
| `FILE_UPLOAD_PATH` | 知识库文档落盘目录 | `uploads/documents/` |
| `CAPTCHA_KEY` / `CAPTCHA_TIMEOUT` | 验证码前缀与有效期（秒） | `captcha_` / `60` |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的前端来源，多个用逗号分隔，支持 `*` 通配本地端口 | `http://localhost:*,http://127.0.0.1:*` |

```powershell
# PowerShell 示例
$env:JWT_SECRET_KEY   = "please-change-me-to-a-random-32-byte-secret"
$env:AI_PROVIDER_SECRET = "another-random-32-byte-secret"
$env:DB_PASSWORD      = "your-db-password"
$env:DOUBAO_API_KEY   = "your-doubao-key"
$env:TAVILY_API_KEY   = "your-tavily-key"
```

## 构建与运行

### 后端

```powershell
cd my-seek
./gradlew.bat build          # 编译 + 单元测试 + 打 jar
./gradlew.bat bootRun        # 本地启动（默认 dev profile）
```

生成的可执行包位于 `build/libs/`。

#### 本地私密配置（推荐）

本地开发使用的真实数据库密码、JWT 密钥等敏感信息**不会提交到仓库**：

1. 复制 `start-local.example.ps1` 为 `start-local.ps1`（该文件已被 `.gitignore` 忽略）。
2. 在 `start-local.ps1` 中填写本机的 `DB_PASSWORD`（及其它需要的凭据）。
3. 右键「使用 PowerShell 运行」，或执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

脚本会自动构建并以 `--spring.profiles.active=dev,local` 启动，同时把控制台和 JVM 输出统一为 UTF-8，避免中文日志在 Windows 上乱码。本地专属配置和真实密码存放在 `src/main/resources/application-local.yml`（同样已被 `.gitignore` 忽略）。

### 前端

```powershell
cd my-seek-web
pnpm install
pnpm dev                     # 开发服务器（Vite 代理到后端）
pnpm build                   # 生产构建（SPA 模式）
pnpm typecheck               # 类型检查
```

## 鉴权说明

- 免鉴权路径：`/api/auth/**`、`/api/open/**`
- 其余 `/api/**`（含 `/api/ai/**` 聊天接口、供应商管理）均需携带 `Authorization: Bearer <token>`
- 会话按用户隔离：聊天记忆的 Redis key 会拼接当前用户 ID，避免跨用户串会话
- 用户管理接口（`/api/user/add`、`/delete`、`/update`、`/query`、角色/组织维护等）需管理员；普通用户返回 403
- `/actuator/health` 公开，其余 actuator 端点需登录

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录（图形验证码） |
| POST | `/api/ai/chat` | 同步对话（可携带联网搜索开关） |
| POST | `/api/ai/chat/stream` | 流式对话（增量文本 / 推理过程） |
| GET | `/api/ai/chat/history` | 会话历史消息 |
| GET | `/api/ai/chat/info` | 会话摘要（标题） |
| GET | `/api/ai/chat/item/list` | 当前用户的会话列表 |
| GET | `/api/ai/models` | 可用模型列表 |
| GET | `/api/ai/providers` | 第三方模型供应商列表 |
| POST | `/api/ai/providers` | 新增供应商（API Key 加密保存） |
| PUT | `/api/ai/providers/{id}` | 更新供应商 |
| DELETE | `/api/ai/providers/{id}` | 删除供应商 |
| PATCH | `/api/ai/providers/{id}/toggle?enabled=` | 启停供应商 |
| POST | `/api/ai/providers/test` | 测试接口连通性 |
| POST | `/api/knowledge/upload` | 上传知识库文档（自动解析 + 向量化） |
| POST | `/api/knowledge/query` | 知识库问答（RAG） |

统一返回结构：

```json
{ "success": true, "code": 200, "msg": null, "data": {} }
```

### 流式对话协议

`POST /api/ai/chat/stream` 不使用统一包装，返回逐行帧：

```text
MYSEEK_FRAME:{"type":"meta","performed":true,"engine":"Tavily","sources":[{"title":"...","url":"..."}]}
MYSEEK_FRAME:{"type":"content","text":"增量文本"}
MYSEEK_FRAME:{"type":"done"}
```

异常结束帧：

```text
MYSEEK_FRAME:{"type":"error","message":"生成失败，请稍后重试"}
```

`content` 帧中的推理标签仍由前端解析；新协议兼容旧版裸文本流和旧版 `MYSEEK_SOURCES` 首帧。

## 生产部署清单

### 1. 必填环境变量

| 变量 | 要求 | 说明 |
| --- | --- | --- |
| `JWT_SECRET_KEY` | 必填，至少 32 字节 | 登录令牌签名密钥 |
| `AI_PROVIDER_SECRET` | 必填 | 供应商 API Key 加密密钥，变更后已有密钥可能无法解密 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 必填 | MariaDB 连接信息 |
| `REDIS_HOST` / `REDIS_PORT` | 必填 | Redis Stack 地址，必须带 RediSearch 模块 |
| `OLLAMA_BASE_URL` | 使用本地模型时必填 | 例如 `http://ollama:11434` |
| `CORS_ALLOWED_ORIGINS` | 建议必填 | 多个前端域名用逗号分隔 |

可选：`WEB_SEARCH_PROVIDER=tavily|wikipedia`、`TAVILY_API_KEY`、`WEB_SEARCH_MAX_RESULTS`、`WEB_SEARCH_TIMEOUT_MS`、`REDIS_PASSWORD`、`VECTOR_STORE_INITIALIZE_SCHEMA`、`MAIL_*`。

### 2. 一键启动

```bash
cp .env.example .env
# 编辑 .env，至少修改 DB_PASSWORD、DB_ROOT_PASSWORD、JWT_SECRET_KEY、AI_PROVIDER_SECRET

docker compose up -d --build
```

compose 会启动 MariaDB、Redis Stack、Ollama、后端和前端：

- Web：`http://localhost:3000`
- 后端：`http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`
- 就绪检查：`http://localhost:8080/actuator/health/readiness`
- 存活检查：`http://localhost:8080/actuator/health/liveness`

### 3. 首次启动检查

1. 确认 Redis Stack 已加载 RediSearch：`docker compose exec redis redis-cli MODULE LIST`。
2. 确认知识库索引已创建：首次部署保持 `VECTOR_STORE_INITIALIZE_SCHEMA=true`。
3. 拉取 Ollama 模型（如使用本地模型）：
   ```bash
   docker compose exec ollama ollama pull deepseek-r1:1.5b
   docker compose exec ollama ollama pull nomic-embed-text
   ```
4. 用管理员账号登录后，在“用户管理”中创建内部用户。
5. 在“AI 接口管理”中添加供应商，并进行连通性测试。
6. 在“知识库”上传一份测试文档，确认状态变为“已完成”。

### 4. 请求链路排查

后端为每个请求生成或透传 `X-Request-Id`，写入日志 MDC 并回写到响应头。日志格式为：

```text
INFO [MySeek,2f6a...] ...
```

前端遇到错误时，可查看浏览器 Network 中对应响应的 `X-Request-Id`，再到后端日志中检索该 ID。

## 最近更新

- 流式对话改为结构化帧协议（meta/content/done/error），前端统一解析并保留旧协议兼容
- 新增第三方 AI 供应商管理：可视化增删启停、API Key 加密入库、连通性测试；模型选择改为运行时从配置动态解析
- 新增联网搜索：Tavily 优先、Wikipedia 自动降级，结果注入上下文并回传来源
- 构建链：Gradle 8.14.3 → 9.9.1，JDK toolchain 21 → 25
- Spring Boot 4.0.5 → 4.0.9，Spring AI 2.0.0-M4 → 2.0.2 GA；OpenAI 兼容模型改用官方 `openai-java` SDK
- Jackson 2 → 3、JJWT 0.11.5 → 0.13.0、Tika 2.9.1 → 4.2.0 全面适配
- 修复 `EncryptUtil` 随机密钥导致「加密后无法解密」的缺陷；新增统一全局异常处理
- 聊天记忆 / 会话列表按用户隔离；知识库文档列表分页 + `document_id` 元数据过滤删除

## 其它

在 PowerShell 中执行以下命令，可以生成空白章节文件：

```powershell
powershell -ExecutionPolicy Bypass -File .\sh\create_chapters.ps1
```

## 开源协议

本项目采用 [MIT License](./LICENSE) 开源协议。
