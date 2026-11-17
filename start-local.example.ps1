# MySeek 本地启动脚本示例（不含真实凭据）
# 用法：
#   1. 复制本文件为 start-local.ps1（该文件已被 .gitignore 忽略）
#   2. 把下面的 DB_PASSWORD 改为本机数据库的真实密码
#   3. 右键 -> 使用 PowerShell 运行，或执行：powershell -ExecutionPolicy Bypass -File .\start-local.ps1

$ErrorActionPreference = 'Stop'

# 统一 UTF-8：Java 18+ 默认用 UTF-8 输出日志，Windows 控制台也需切到 UTF-8 才能正确解码
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# ---- 请填写本机真实凭据 ----
$env:DB_PASSWORD = 'change-me'
# $env:REDIS_PASSWORD = ''
# $env:JWT_SECRET_KEY = 'local-dev-only-secret-key-please-change-me-32bytes'

Write-Host '正在构建项目...' -ForegroundColor Cyan
Push-Location $PSScriptRoot
try {
    & .\gradlew.bat build -q
    if ($LASTEXITCODE -ne 0) { throw '构建失败' }

    $jar = Get-ChildItem 'build\libs' -Filter 'my-seek-*.jar' |
           Where-Object { $_.Name -notlike '*plain*' } |
           Select-Object -First 1
    if (-not $jar) { throw '未找到可执行 jar' }

    Write-Host "使用 local 配置启动: $($jar.Name)" -ForegroundColor Green
    & java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -jar $jar.FullName '--spring.profiles.active=dev,local'
}
finally {
    Pop-Location
}