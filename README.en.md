# MySeek

[![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.9-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

**MySeek** is a self-hosted AI chat platform for enterprises. It unifies local models, cloud LLMs, private knowledge bases and live web search behind one authenticated API.

> Web client: [my-seek-web](https://github.com/guwan/my-seek-web) · Website: [my-seek-page](https://github.com/guwan/my-seek-page) · Chinese docs: [README.md](./README.md)

## Highlights

- **Multi-model**: Ollama (DeepSeek, etc.), Doubao, Qwen and any OpenAI-compatible endpoint, switchable per request.
- **Provider management**: add, edit, enable/disable third-party AI providers from the UI; API keys are encrypted at rest.
- **RAG knowledge base**: upload documents, auto-parse and vectorize, then ask questions grounded in your own data (Redis Vector Store).
- **Web search**: optional real-time search with Tavily (API key) and automatic Wikipedia fallback, with source links returned to the client.
- **Secure by design**: JWT auth, captcha, per-user isolated chat memory and history; all secrets injected via environment variables.
- **Streaming chat**: incremental SSE output with reasoning display and auto-generated conversation titles.

## Quick start

```bash
# Backend (Java 25, MariaDB/MySQL, Redis)
./gradlew bootRun

# Frontend (Node.js 22+, pnpm)
cd ../my-seek-web
pnpm install && pnpm dev
```

See [README.md](./README.md) for the full environment-variable reference, API list and deployment notes.

## License

[MIT](./LICENSE)
