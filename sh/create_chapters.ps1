# 定义目标目录
$targetDir = "D:\git\my-seek\doc\char"

# 确保目录存在，不存在则创建
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir | Out-Null
}

# 循环创建文件
for ($i = 22; $i -le 120; $i++) {
    $filePath = Join-Path $targetDir ("章节{0}.md" -f $i)
    New-Item -ItemType File -Path $filePath -Force | Out-Null
}

Write-Host "已生成 章节22.md 到 章节120.md" -ForegroundColor Green
