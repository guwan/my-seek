# MySeek

[English](README.md) | [简体中文](README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.9-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.2-6DB33F)](https://spring.io/projects/spring-ai)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/guwan/my-seek?style=social)](https://github.com/guwan/my-seek)

**MySeek** is a self-hosted AI chat workspace backend for everyone. It unifies local models, cloud LLMs, private knowledge bases, and live web search behind one API, with built-in authentication, per-user isolation, and visual model provider management.

> Web workspace: [my-seek-web](https://github.com/guwan/my-seek-web) · Landing page: [my-seek-page](https://github.com/guwan/my-seek-page)
>
> Development progress: [TASKS.md](./TASKS.md)

## One-line Demo

The fastest way to try MySeek — in a terminal with Git Bash (Windows) / macOS / Linux:

```bash
curl -fsSL https://raw.githubusercontent.com/guwan/my-seek/main/doc/demo.sh | bash
```

The script checks `git`, JDK 25+, Node.js 22+ and pnpm 11+ (missing tools cannot be skipped), asks for the download directory, clones or updates all three repositories, then offers **auto or skip** for the database (auto = embedded H2 file database) and Redis (auto = embedded Luban-RDS). **No Docker, MariaDB or Redis Stack is required.** Ollama is never downloaded automatically: the script prints install and `ollama pull` instructions instead. It then starts the backend with the `demo` profile + web app and opens `http://localhost:5173`.

Re-running is safe: existing checkouts are updated with `git pull --ff-only`, and H2/Luban-RDS data is reused from `.myseek-demo/`. For non-interactive runs set `MYSEEK_DEMO_DIR`, `MYSEEK_DEMO_DB`, `MYSEEK_DEMO_REDIS` (values: `auto` / `skip`). Demo mode disables knowledge-base RAG because Luban-RDS does not implement RediSearch; use Redis Stack for full RAG. Full script: [`doc/demo.sh`](./doc/demo.sh).

---

## Highlights

- **Unified multi-model access**: local Ollama (DeepSeek, etc.), Doubao, Qwen, and any OpenAI-compatible endpoint, switchable per request.
- **Visual provider management**: add, edit, enable/disable third-party AI providers from the UI. API keys are encrypted at rest and changes take effect without restart.
- **Web search augmentation**: optional real-time search with Tavily (API key required) first and automatic Wikipedia fallback. Results are injected into context and source links are returned to the client.
- **RAG knowledge base**: document upload, automatic parsing and vectorization, retrieval-augmented answers backed by Redis Vector Store, paginated management, and vector deletion by `document_id`.
- **Security and isolation**: JWT authentication plus captcha; chat memory and conversation lists are isolated per user; all sensitive configuration is injected through environment variables.
- **Streaming chat**: SSE incremental output, automatic conversation titles, time grouping, and reasoning display.
- **Engineering**: virtual threads for IO concurrency, graceful shutdown, unified global exception handling, and unit tests that run without a database or Redis.

## Project Structure

| Repository | Description | Main Stack |
| --- | --- | --- |
| [guwan/my-seek](https://github.com/guwan/my-seek) | Backend: auth, chat, knowledge base, web search, provider management, AI task center | Java 25 / Spring Boot 4 / Spring AI 2 |
| [guwan/my-seek-web](https://github.com/guwan/my-seek-web) | Web workspace: multi-model chat, conversation management, AI writing, task center | React 19 / React Router 7 / Redux Toolkit |
| [guwan/my-seek-page](https://github.com/guwan/my-seek-page) | Landing page: product intro, features, docs entry | Vite 6 / React 19 / Tailwind CSS 4 |

## Preview

<p align="center">
  <img src="https://raw.githubusercontent.com/guwan/my-seek-web/main/assets/image.png" alt="MySeek Web chat UI" width="920" />
</p>

---

## Tech Stack

| Layer | Technology | Version |
| --- | --- | --- |
| Backend framework | Spring Boot | 4.0.9 |
| AI orchestration | Spring AI | 2.0.2 (GA) |
| ORM | Spring Data JPA / Hibernate | Bundled with Boot 4.0.9 |
| Serialization | Jackson | 3.x (Spring Boot 4 default) |
| Security | Spring Security + JJWT | 7.x / 0.13.0 |
| Vector store | Redis Vector Store (RediSearch) | Bundled with Spring AI |
| Build | Gradle | 9.9.1 (wrapper included) |
| JDK | Java | 25 (toolchain resolved automatically) |
| Frontend | React + React Router | 19.3.1 / 7.18.5 |
| Frontend build | Vite + Tailwind CSS | 7.3.8 / 4.3.5 |
| Package manager | pnpm | 11.x |

## Requirements

- JDK 17+ (the project toolchain is fixed to 25; Gradle uses an installed JDK 25 automatically)
- Node.js 24+ (pnpm)
- MariaDB / MySQL 8+
- Redis 6+
- (Optional) Ollama local model service

> **Startup order**: MariaDB → Redis → Ollama → application.
> The application can start without Redis in development, but captcha, chat memory, and the knowledge base will fail.
>
> **Extra RAG requirements**:
> 1. Redis must include the RediSearch module (Redis Stack). Plain Redis cannot execute `FT.*` commands.
> 2. Install the embedding model in Ollama: `ollama pull nomic-embed-text` (override with `OLLAMA_EMBEDDING_MODEL`).

## Key Environment Variables

All sensitive configuration is injected through environment variables and never hardcoded.

| Variable | Description | Default |
| --- | --- | --- |
| `JWT_SECRET_KEY` | JWT signing secret, at least 32 bytes; **production fails to start if missing**; dev generates a temporary key | None (required in prod) |
| `AI_PROVIDER_SECRET` | Key used to encrypt provider API keys; **must be overridden in production** | Dev default only |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Database connection | `jdbc:mariadb://localhost:3306/seek` / `root` / empty |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis connection | `localhost` / `6379` / empty |
| `DOUBAO_API_KEY` / `DOUBAO_BASE_URL` | Doubao model | empty / Volcano Ark endpoint |
| `QWEN_API_KEY` / `QWEN_BASE_URL` | Qwen model | empty / DashScope-compatible endpoint |
| `OLLAMA_BASE_URL` / `OLLAMA_CHAT_MODEL` | Local model | `http://localhost:11434` / `deepseek-r1:8b` |
| `WEB_SEARCH_PROVIDER` / `TAVILY_API_KEY` / `TAVILY_BASE_URL` | Web search: provider is `tavily` or `wikipedia`; empty key falls back automatically | `tavily` / empty / Tavily endpoint |
| `WEB_SEARCH_MAX_RESULTS` | Max results per search | `5` |
| `MAIL_HOST` / `MAIL_ACCOUNT` / `MAIL_TOKEN` | Mail service | empty |
| `FILE_UPLOAD_PATH` | Knowledge base document storage path | `uploads/documents/` |
| `CAPTCHA_KEY` / `CAPTCHA_TIMEOUT` | Captcha prefix and TTL (seconds) | `captcha_` / `60` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins, comma-separated; supports `*` for local ports | `http://localhost:*,http://127.0.0.1:*` |

```powershell
# PowerShell example
$env:JWT_SECRET_KEY     = "please-change-me-to-a-random-32-byte-secret"
$env:AI_PROVIDER_SECRET = "another-random-32-byte-secret"
$env:DB_PASSWORD        = "your-db-password"
$env:DOUBAO_API_KEY     = "your-doubao-key"
$env:TAVILY_API_KEY     = "your-tavily-key"
```

## Build and Run

### Backend

```powershell
cd my-seek
./gradlew.bat build          # compile + unit tests + jar
./gradlew.bat bootRun        # local start (dev profile by default)
```

The executable package is written to `build/libs/`.

#### Local Private Configuration (Recommended)

Real database passwords, JWT secrets, and similar values used in local development are **not committed**:

1. Copy `start-local.example.ps1` to `start-local.ps1` (ignored by `.gitignore`).
2. Fill in your local `DB_PASSWORD` and any other credentials in `start-local.ps1`.
3. Right-click and **Run with PowerShell**, or execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

The script builds the project and starts it with `--spring.profiles.active=dev,local`, normalizing console and JVM output to UTF-8 to avoid garbled Chinese logs on Windows. Local-only configuration and real secrets live in `src/main/resources/application-local.yml` (also ignored by `.gitignore`).

### Frontend

```powershell
cd my-seek-web
pnpm install
pnpm dev                     # dev server (Vite proxies to backend)
pnpm build                   # production SPA build
pnpm typecheck               # type check
```

## Authentication

- Public paths: `/api/auth/**`, `/api/open/**`
- All other `/api/**` endpoints (including `/api/ai/**` chat and provider management) require `Authorization: Bearer <token>`
- Conversations are isolated per user: Redis memory keys include the current user ID
- User management endpoints (`/api/user/add`, `/delete`, `/update`, `/query`, role/org maintenance, etc.) require admin; regular users receive 403
- `/actuator/health` is public; other actuator endpoints require login

## Main APIs

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/auth/login` | Login with captcha |
| POST | `/api/ai/chat` | Synchronous chat (optional web search) |
| POST | `/api/ai/chat/stream` | Streaming chat (incremental text / reasoning) |
| GET | `/api/ai/chat/history` | Conversation history |
| GET | `/api/ai/chat/info` | Conversation summary (title) |
| GET | `/api/ai/chat/item/list` | Current user's conversation list |
| GET | `/api/ai/models` | Available models |
| GET | `/api/ai/providers` | Third-party provider list |
| POST | `/api/ai/providers` | Add provider (API key encrypted) |
| PUT | `/api/ai/providers/{id}` | Update provider |
| DELETE | `/api/ai/providers/{id}` | Delete provider |
| PATCH | `/api/ai/providers/{id}/toggle?enabled=` | Enable/disable provider |
| POST | `/api/ai/providers/test` | Test provider connectivity |
| POST | `/api/knowledge/upload` | Upload knowledge document (parse + vectorize) |
| POST | `/api/knowledge/query` | Knowledge base QA (RAG) |

Unified response envelope:

```json
{ "success": true, "code": 200, "msg": null, "data": {} }
```

### Streaming Chat Protocol

`POST /api/ai/chat/stream` does not use the unified envelope. It returns newline-delimited frames:

```text
MYSEEK_FRAME:{"type":"meta","performed":true,"engine":"Tavily","sources":[{"title":"...","url":"..."}]}
MYSEEK_FRAME:{"type":"content","text":"incremental text"}
MYSEEK_FRAME:{"type":"done"}
```

Error frame:

```text
MYSEEK_FRAME:{"type":"error","message":"Generation failed, please retry"}
```

Reasoning tags inside `content` frames are parsed by the frontend. The new protocol remains compatible with the legacy raw-text stream and the old `MYSEEK_SOURCES` first frame.

## Production Deployment Checklist

### 1. Required Environment Variables

| Variable | Requirement | Description |
| --- | --- | --- |
| `JWT_SECRET_KEY` | Required, at least 32 bytes | Token signing secret |
| `AI_PROVIDER_SECRET` | Required | Provider API key encryption secret; changing it may make existing keys undecryptable |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Required | MariaDB connection |
| `REDIS_HOST` / `REDIS_PORT` | Required | Redis Stack address with RediSearch |
| `OLLAMA_BASE_URL` | Required when using local models | e.g. `http://ollama:11434` |
| `CORS_ALLOWED_ORIGINS` | Recommended | Comma-separated frontend origins |

Optional: `WEB_SEARCH_PROVIDER=tavily|wikipedia`, `TAVILY_API_KEY`, `WEB_SEARCH_MAX_RESULTS`, `WEB_SEARCH_TIMEOUT_MS`, `REDIS_PASSWORD`, `VECTOR_STORE_INITIALIZE_SCHEMA`, `MAIL_*`.

### 2. One-command Start

```bash
cp .env.example .env
# Edit .env and at least change DB_PASSWORD, DB_ROOT_PASSWORD, JWT_SECRET_KEY, AI_PROVIDER_SECRET

docker compose up -d --build
```

Compose starts MariaDB, Redis Stack, Ollama, backend, and frontend:

- Web: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- Liveness: `http://localhost:8080/actuator/health/liveness`

### 3. First-start Checks

1. Verify RediSearch is loaded: `docker compose exec redis redis-cli MODULE LIST`.
2. Keep `VECTOR_STORE_INITIALIZE_SCHEMA=true` on first deployment to create the vector index.
3. Pull Ollama models if using local models:
   ```bash
   docker compose exec ollama ollama pull deepseek-r1:1.5b
   docker compose exec ollama ollama pull nomic-embed-text
   ```
4. Log in with an admin account and create internal users under **User Management**.
5. Add a provider in **AI Provider Management** and run a connectivity test.
6. Upload a test document in **Knowledge Base** and confirm it becomes **Completed**.
7. Confirm Flyway applied the migrations from `db/migration` in the backend logs. Production uses `ddl-auto=validate`, so future schema changes must be added as new migration scripts.

### 4. Request Tracing

The backend generates or forwards an `X-Request-Id` for every request, writes it to MDC, and returns it in the response header. Log format:

```text
INFO [MySeek,2f6a...] ...
```

When the frontend reports an error, copy the `X-Request-Id` from the Network response header and search for it in backend logs.

## Recent Changes

- Structured streaming protocol (meta/content/done/error), unified frontend parsing with legacy compatibility
- Visual AI provider management: add/edit/enable-disable, encrypted API keys, connectivity tests, runtime model resolution
- Web search: Tavily first with automatic Wikipedia fallback, context injection, and source links
- Build chain: Gradle 8.14.3 → 9.9.1, JDK toolchain 21 → 25
- Spring Boot 4.0.5 → 4.0.9, Spring AI 2.0.0-M4 → 2.0.2 GA; OpenAI-compatible models now use the official `openai-java` SDK
- Jackson 2 → 3, JJWT 0.11.5 → 0.13.0, Tika 2.9.1 → 4.2.0
- Fixed `EncryptUtil` random-key encryption/decryption defect; added unified global exception handling
- Per-user chat memory and conversation lists; paginated knowledge documents with `document_id` metadata filtering for deletion

## Other

Run the following in PowerShell to generate empty chapter files:

```powershell
powershell -ExecutionPolicy Bypass -File .\sh\create_chapters.ps1
```

## License

This project is licensed under the [MIT License](./LICENSE).
