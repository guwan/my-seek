#!/usr/bin/env bash
# MySeek 一键 Demo（模拟 curl | bash 体验）：
#   curl -fsSL https://raw.githubusercontent.com/guwan/my-seek/main/doc/demo.sh | bash
# 本地执行：bash doc/demo.sh
# 自动处理：数据库 = H2 文件库；Redis = 内嵌 Luban-RDS（均无需 Docker）。Ollama 需用户自行安装。
# 可选环境变量（非交互）：MYSEEK_DEMO_DIR、MYSEEK_DEMO_DB、MYSEEK_DEMO_REDIS（值为 auto / skip）
#                      REDIS_PORT、OLLAMA_BASE_URL、OLLAMA_CHAT_MODEL、OLLAMA_EMBEDDING_MODEL
set -Eeuo pipefail

C_RED=$'\033[0;31m'; C_GREEN=$'\033[0;32m'; C_YELLOW=$'\033[1;33m'; C_CYAN=$'\033[0;36m'; C_OFF=$'\033[0m'
info() { printf '%s[MySeek]%s %s\n' "$C_CYAN" "$C_OFF" "$*"; }
ok()   { printf '%s[ OK ]%s %s\n' "$C_GREEN" "$C_OFF" "$*"; }
warn() { printf '%s[WARN]%s %s\n' "$C_YELLOW" "$C_OFF" "$*" >&2; }
die()  { printf '%s[FAIL]%s %s\n' "$C_RED" "$C_OFF" "$*" >&2; exit 1; }

# curl | bash 时 stdin 是脚本管道，交互输入统一走 /dev/tty
if exec 3</dev/tty 2>/dev/null; then
  :
elif [ -t 0 ]; then
  exec 3<&0
else
  die '需要交互式终端：请在终端中直接执行，不要重定向标准输入'
fi

ask() {
  local answer=''
  printf '%s [%s]: ' "$1" "${2:-}" >&2
  IFS= read -r answer <&3 || answer=''
  printf '%s' "${answer:-${2:-}}"
}

major_version() {
  local s=${1#v}
  s=${s//[^0-9.]/}
  s=${s%%.*}
  printf '%s' "$s"
}

port_open() {
  local port=$1
  (exec 3<>"/dev/tcp/127.0.0.1/$port") >/dev/null 2>&1 && return 0
  return 1
}

wait_port() {
  local port=$1 name=$2 timeout=${3:-120} i=0
  until port_open "$port"; do
    i=$((i + 5))
    if [ "$i" -ge "$timeout" ]; then die "$name 在 ${timeout}s 内未就绪（端口 $port）"; fi
    printf '.' >&2
    sleep 5
  done
  printf '\n' >&2
  ok "$name 已就绪（端口 $port）"
}

confirm_dep() {
  local label=$1 var=$2 choice=${!2:-}
  if [ -z "$choice" ]; then
    printf '\n==== %s ====\n  1) 由脚本自动处理\n  2) 跳过（我稍后自行处理）\n' "$label" >&2
    choice=$(ask '请选择 1/2' '1')
  fi
  case "$choice" in
    1|auto|AUTO|Auto) printf 'auto' ;;
    2|skip|SKIP|Skip) printf 'skip' ;;
    *) die "无效选择：$choice（仅支持 1/auto 或 2/skip）" ;;
  esac
}

# ---------- 1. 环境检查（缺失不可跳过） ----------
info '检查运行环境（git / JDK / Node.js / pnpm）...'
missing=()

check_git() {
  if ! command -v git >/dev/null 2>&1; then
    missing+=('git 未安装：https://git-scm.com/downloads')
    return
  fi
  ok "git $(git --version | sed 's/^git version //')"
}

check_jdk() {
  if ! command -v javac >/dev/null 2>&1; then
    missing+=('JDK 未安装或未加入 PATH（必须有 javac，仅 JRE 不够）：https://openjdk.org/install/')
    return
  fi
  local raw v
  raw=$(java -version 2>&1 | head -n 1 || true)
  v=$(major_version "$raw")
  if [ -z "$v" ] || [ "$v" -lt 25 ]; then
    missing+=("JDK 版本不满足：$raw，需要 JDK 25+（Gradle toolchain 固定 25）")
    return
  fi
  ok "JDK $raw"
}

check_node() {
  if ! command -v node >/dev/null 2>&1; then
    missing+=('Node.js 未安装：https://nodejs.org/（建议 24.x）')
    return
  fi
  local raw v
  raw=$(node --version 2>&1 | head -n 1 || true)
  v=$(major_version "$raw")
  if [ -z "$v" ] || [ "$v" -lt 22 ]; then
    missing+=("Node.js 版本不满足：$raw，需要 22+（建议 24.x）")
    return
  fi
  [ "$v" -ge 24 ] || warn 'Node.js 22 可用，但推荐 24.x（my-seek-page 要求 24+）'
  ok "Node.js $raw"
}

check_pnpm() {
  if ! command -v pnpm >/dev/null 2>&1; then
    missing+=('pnpm 未安装：corepack enable && corepack prepare pnpm@11 --activate')
    return
  fi
  local raw v
  raw=$(pnpm --version 2>&1 | head -n 1 || true)
  v=$(major_version "$raw")
  if [ -z "$v" ] || [ "$v" -lt 11 ]; then
    missing+=("pnpm 版本不满足：$raw，需要 11+")
    return
  fi
  ok "pnpm $raw"
}

command -v curl >/dev/null 2>&1 || die 'curl 未安装，请先安装 curl（本脚本需要它下载脚本并探测服务）'
check_git; check_jdk; check_node; check_pnpm

if [ "${#missing[@]}" -gt 0 ]; then
  printf '\n%s环境检查未通过，以下依赖缺失且不可跳过：%s\n' "$C_RED" "$C_OFF" >&2
  for item in "${missing[@]}"; do printf '  - %s\n' "$item" >&2; done
  die '安装上述依赖并加入 PATH 后重新运行本脚本'
fi
ok '环境检查通过'

# ---------- 2. 代码获取 ----------
printf '\n请选择代码下载目录：\n  1) 当前目录：%s\n  2) 自定义目录\n' "$PWD" >&2
case "$(ask '请选择 1/2' '1')" in
  1) target=$PWD ;;
  2)
    raw_dir=$(ask '请输入目录路径（支持 ~）' "$PWD/myseek-demo")
    target=${raw_dir/#\~/$HOME}
    ;;
  *) die '无效选择，请输入 1 或 2' ;;
esac
mkdir -p "$target" || die "无法创建目录：$target"
ROOT_DIR=$(cd "$target" && pwd -P) || die "无法进入目录：$target"
ok "代码目录：$ROOT_DIR"

sync_repo() {
  local name=$1 url=$2 dir="$ROOT_DIR/$1"
  if [ -d "$dir/.git" ]; then
    info "检测到已有代码，拉取更新：$name"
    if git -C "$dir" pull --ff-only; then
      ok "$name 已更新到最新版本"
    else
      warn "$name 更新失败（可能有本地改动或远端已分叉），继续使用现有代码"
    fi
  elif [ -e "$dir" ]; then
    die "$dir 已存在但不是 Git 仓库；为避免覆盖，请先移走该目录"
  else
    info "克隆代码：$name"
    git clone --depth 1 "$url" "$dir" || die "克隆失败：$url"
    ok "$name 克隆完成"
  fi
}

sync_repo my-seek      https://github.com/guwan/my-seek.git
sync_repo my-seek-web  https://github.com/guwan/my-seek-web.git
sync_repo my-seek-page https://github.com/guwan/my-seek-page.git

# ---------- 3. 依赖处理（H2 + 内嵌 Luban-RDS，无需 Docker） ----------
WORK_DIR=${MYSEEK_WORK_DIR:-"$ROOT_DIR/.myseek-demo"}
mkdir -p "$WORK_DIR" || die "无法创建工作目录：$WORK_DIR"
export MYSEEK_WORK_DIR="$WORK_DIR"
info "Demo 数据目录：$WORK_DIR（H2 数据库与 Luban-RDS RDB 均在此处，重复运行会复用）"

db_choice=$(confirm_dep '数据库（自动 = H2 文件库 / 跳过 = 自带 MariaDB、MySQL）' MYSEEK_DEMO_DB)
if [ "$db_choice" = auto ]; then
  h2_path="$WORK_DIR/demo-db/seek"
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
      if command -v cygpath >/dev/null 2>&1; then h2_path=$(cygpath -m "$h2_path"); fi
      ;;
  esac
  export DB_DRIVER=org.h2.Driver
  export DB_URL="jdbc:h2:file:${h2_path};MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_ON_EXIT=FALSE"
  export DB_USERNAME=sa
  export DB_PASSWORD=
  ok "数据库：H2 文件库（$h2_path.mv.db）"
else
  export DB_DRIVER=${DB_DRIVER:-org.mariadb.jdbc.Driver}
  warn '数据库已跳过：请自行启动 MariaDB/MySQL，并保证库 seek 与 DB_URL/DB_USERNAME/DB_PASSWORD 一致'
fi

redis_choice=$(confirm_dep 'Redis（自动 = 内嵌 Luban-RDS / 跳过 = 自带 Redis Stack）' MYSEEK_DEMO_REDIS)
redis_port_explicit=${REDIS_PORT:+1}
REDIS_PORT=${REDIS_PORT:-9736}
if [ "$redis_choice" = auto ]; then
  if [ -z "$redis_port_explicit" ] && port_open "$REDIS_PORT"; then
    for candidate in 9736 9737 9738 9739 9740; do
      if ! port_open "$candidate"; then REDIS_PORT=$candidate; break; fi
    done
  fi
  if port_open "$REDIS_PORT"; then
    warn "端口 $REDIS_PORT 已被占用，不再内嵌 Luban-RDS，改为连接现有服务"
    export LUBAN_RDS_ENABLED=false
  else
    export LUBAN_RDS_ENABLED=true
    info "Redis：内嵌 Luban-RDS，端口 $REDIS_PORT（随后端进程停止）"
  fi
  export REDIS_HOST=localhost
else
  export LUBAN_RDS_ENABLED=false
  export REDIS_HOST=${REDIS_HOST:-localhost}
  warn 'Redis 已跳过：请自行启动 Redis Stack（需 RediSearch），并设置 REDIS_HOST/REDIS_PORT'
fi
export REDIS_PORT

# Ollama：不做自动下载，仅提示
printf '\n==== Ollama 本地模型服务 ====\n' >&2
warn 'Demo 不会自动安装 Ollama 或下载模型（单个模型通常 1GB+），请自行处理：'
printf '  1) 安装 Ollama：https://ollama.com/download\n  2) 拉取模型：ollama pull deepseek-r1:1.5b\n  3) 拉取嵌入模型：ollama pull nomic-embed-text\n' >&2
export OLLAMA_BASE_URL=${OLLAMA_BASE_URL:-http://localhost:11434}
export OLLAMA_CHAT_MODEL=${OLLAMA_CHAT_MODEL:-deepseek-r1:1.5b}
export OLLAMA_EMBEDDING_MODEL=${OLLAMA_EMBEDDING_MODEL:-nomic-embed-text}

# ---------- 4. 启动与访问 ----------
export DB_URL DB_DRIVER DB_USERNAME DB_PASSWORD DB_ROOT_PASSWORD
export REDIS_PASSWORD=${REDIS_PASSWORD:-}
export VECTOR_STORE_INITIALIZE_SCHEMA=false
export JWT_SECRET_KEY=${JWT_SECRET_KEY:-myseek-demo-jwt-secret-key-at-least-32-bytes}
export AI_PROVIDER_SECRET=${AI_PROVIDER_SECRET:-myseek-demo-ai-provider-secret}
export WEB_SEARCH_PROVIDER=${WEB_SEARCH_PROVIDER:-wikipedia}
export CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-http://localhost:5173}
export SPRING_PROFILES_ACTIVE=demo

LOG_DIR="$WORK_DIR/logs"
mkdir -p "$LOG_DIR"
GRADLEW=gradlew
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) GRADLEW=gradlew.bat ;; esac

info '启动后端（首次构建需下载依赖，可能需要几分钟）...'
(cd "$ROOT_DIR/my-seek" && nohup "./$GRADLEW" bootRun) >"$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
wait_port 8080 '后端' 300

info '安装前端依赖并启动 Web...'
(cd "$ROOT_DIR/my-seek-web" && pnpm install) >"$LOG_DIR/web-install.log" 2>&1 || die "前端依赖安装失败，请查看 $LOG_DIR/web-install.log"
(cd "$ROOT_DIR/my-seek-web" && nohup pnpm dev --host 127.0.0.1 --port 5173 --strictPort) >"$LOG_DIR/web.log" 2>&1 &
WEB_PID=$!
wait_port 5173 'Web 前端' 180

WEB_URL='http://localhost:5173'
info "自动打开浏览器：$WEB_URL"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) cmd.exe /c start "" "$WEB_URL" >/dev/null 2>&1 || true ;;
  Darwin*) open "$WEB_URL" >/dev/null 2>&1 || true ;;
  *) xdg-open "$WEB_URL" >/dev/null 2>&1 || true ;;
esac

printf '\n%s==== MySeek Demo 已启动 ====%s\n' "$C_GREEN" "$C_OFF"
printf '  Web 工作台：%s\n' "$WEB_URL"
printf '  后端接口  ：http://localhost:8080/actuator/health\n'
printf '  数据目录  ：%s\n' "$WORK_DIR"
printf '  日志目录  ：%s\n' "$LOG_DIR"
printf '  宣传页    ：cd %s && pnpm dev（默认 5173，与 Web 冲突时可加 --port 5174）\n' "$ROOT_DIR/my-seek-page"
printf '  停止应用  ：kill %s %s（内嵌 Luban-RDS 会随后端一起停止）\n' "$BACKEND_PID" "$WEB_PID"
warn 'Demo 模式使用 H2 + Luban-RDS，未使用 Redis Stack，知识库 RAG（向量检索）不可用'
info '首次使用请在登录页注册管理员账号，并在“AI 接口管理”中配置模型（或按提示自行安装 Ollama）。'
