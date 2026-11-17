# MySeek

为企事业内部提供的 DeepSeek 接入层，支持自定义知识库（RAG）、多模型（本地 Ollama / 豆包 / 千问）对话与统一鉴权。

---

## 技术栈

| 层 | 技术 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot | 4.0.9 |
| AI 编排 | Spring AI | 2.0.2 (GA) |
| ORM | Spring Data JPA / Hibernate | 随 Boot 4.0.9 |
| 序列化 | Jackson | 3.x（Spring Boot 4 默认） |
| 安全 | Spring Security + JJWT | 7.x / 0.13.0 |
| 构建 | Gradle | 9.9.1（wrapper 已内置） |
| JDK | Java | 25（toolchain 自动解析） |
| 前端 | React + React Router | 19.3.1 / 7.18.5 |
| 前端构建 | Vite + Tailwind CSS | 7.3.8 / 4.3.5 |
| 包管理 | pnpm | 11.x |

## 环境依赖

- JDK 17+（项目 toolchain 固定为 25，Gradle 会自动使用本机已安装的 JDK 25）
- Node.js 22+（pnpm）
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

所有敏感配置均通过环境变量注入，不再硬编码在配置文件里。

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `JWT_SECRET_KEY` | JWT 签名密钥，至少 32 字节；**生产环境未配置时启动即失败**，开发环境会自动生成临时密钥 | 无（prod 必填） |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 数据库连接 | `jdbc:mariadb://localhost:3306/seek` / `root` / 空 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 | `localhost` / `6379` / 空 |
| `DOUBAO_API_KEY` / `DOUBAO_BASE_URL` | 豆包模型 | 空 / 火山方舟地址 |
| `QWEN_API_KEY` / `QWEN_BASE_URL` | 千问模型 | 空 / DashScope 兼容地址 |
| `OLLAMA_BASE_URL` / `OLLAMA_CHAT_MODEL` | 本地模型 | `http://localhost:11434` / `deepseek-r1:8b` |
| `MAIL_HOST` / `MAIL_ACCOUNT` / `MAIL_TOKEN` | 邮件服务 | 空 |
| `FILE_UPLOAD_PATH` | 知识库文档落盘目录 | `uploads/documents/` |
| `CAPTCHA_KEY` / `CAPTCHA_TIMEOUT` | 验证码前缀与有效期（秒） | `captcha_` / `60` |

```powershell
# PowerShell 示例
$env:JWT_SECRET_KEY = "please-change-me-to-a-random-32-byte-secret"
$env:DB_PASSWORD   = "your-db-password"
$env:DOUBAO_API_KEY = "your-doubao-key"
```

## 构建与运行

### 后端

```powershell
cd my-seek
./gradlew.bat build          # 编译 + 单元测试 + 打 jar
./gradlew.bat bootRun        # 本地启动（默认 dev profile）
```

生成的可执行包位于 `build/libs/`。

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
- 其余 `/api/**`（含 `/api/ai/**` 聊天接口）均需携带 `Authorization: Bearer <token>`
- 会话按用户隔离：聊天记忆的 Redis key 会拼接当前用户 ID，避免跨用户串会话
- `/actuator/health` 公开，其余 actuator 端点需登录

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录（图形验证码） |
| POST | `/api/ai/chat` | 同步对话 |
| POST | `/api/ai/chat/stream` | 流式对话（增量文本） |
| GET | `/api/ai/chat/history` | 会话历史消息 |
| GET | `/api/ai/chat/info` | 会话摘要（标题） |
| GET | `/api/ai/chat/item/list` | 当前用户的会话列表 |
| GET | `/api/ai/models` | 可用模型列表 |
| POST | `/api/knowledge/upload` | 上传知识库文档（自动解析 + 向量化） |
| POST | `/api/knowledge/query` | 知识库问答（RAG） |

统一返回结构：

```json
{ "success": true, "code": 200, "msg": null, "data": {} }
```

## 升级说明（本次改动）

- 构建链：Gradle 8.14.3 → 9.9.1，JDK toolchain 21 → 25
- Spring Boot 4.0.5 → 4.0.9，Spring AI 2.0.0-M4 → 2.0.2 GA
  - 记忆 Advisor 改为按请求注入会话 ID（`ChatMemory.CONVERSATION_ID`），修复用户消息/AI 回复重复落库
  - OpenAI 兼容模型（豆包 / 千问）改用官方 `openai-java` SDK
- Jackson 2 → Jackson 3：全部序列化器切换为 `GenericJacksonJsonRedisSerializer` / `JacksonJsonRedisSerializer`
- JJWT 0.11.5 → 0.13.0：改为 `Jwts.SIG.HS256` + `Keys.hmacShaKeyFor`，密钥缺失时启动即失败
- Tika 2.9.1 → 4.2.0：改用 `TikaInputStream`，并限制单文档最大解析长度
- 修复 `EncryptUtil` 随机密钥导致「加密后无法解密」的缺陷（AES/CBC + 随机 IV + SHA-256 派生密钥）
- 新增统一全局异常处理，`Result.unauthorized()` 不再返回 `success=true`
- 聊天记忆 / 会话列表按用户隔离，`/api/ai/**` 需要登录
- 知识库文档列表改分页查询，新增 `document_id` 元数据过滤删除向量
- 新增 13 个单元测试（无需数据库/Redis 即可运行）

## 其它

在 PowerShell 中执行 ，可以生成空白章节文件

```powershell
powershell -ExecutionPolicy Bypass -File D:\git\my-seek\sh\create_chapters.ps1
```
