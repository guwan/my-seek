param(
    [ValidateSet('my-seek', 'my-seek-web', 'my-seek-page')]
    [string[]]$Repo = @('my-seek', 'my-seek-web', 'my-seek-page'),

    [switch]$Tags,
    [switch]$ForceWithLease,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

$mySeekDir = Split-Path -Parent $PSScriptRoot
$workspaceDir = Split-Path -Parent $mySeekDir

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)][string]$RepoPath,
        [Parameter(Mandatory = $true)][string[]]$GitArgs
    )

    & git -C $RepoPath @GitArgs
    if ($LASTEXITCODE -ne 0) {
        throw "git $($GitArgs -join ' ') 执行失败，退出码 $LASTEXITCODE"
    }
}

foreach ($repoName in $Repo) {
    $repoPath = Join-Path $workspaceDir $repoName
    Write-Host "`n=== $repoName ===" -ForegroundColor Cyan

    $insideWorkTree = & git -C $repoPath rev-parse --is-inside-work-tree 2>$null
    if ($LASTEXITCODE -ne 0 -or $insideWorkTree -ne 'true') {
        Write-Warning "不是有效的 Git 仓库，已跳过：$repoPath"
        continue
    }

    $giteePushUrl = & git -C $repoPath config --get remote.gitee.pushurl 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $giteePushUrl) {
        $giteePushUrl = & git -C $repoPath config --get remote.gitee.url 2>$null
    }
    if ($LASTEXITCODE -ne 0 -or -not $giteePushUrl -or $giteePushUrl -eq 'no-pull') {
        Write-Error "未找到有效的 gitee 推送地址：$repoPath"
        continue
    }

    $branch = & git -C $repoPath rev-parse --abbrev-ref HEAD
    if ($LASTEXITCODE -ne 0 -or $branch -eq 'HEAD') {
        Write-Error "当前处于 detached HEAD，已跳过：$repoPath"
        continue
    }

    $upstreamRemote = & git -C $repoPath config --get "branch.$branch.remote" 2>$null
    if ($LASTEXITCODE -eq 0 -and $upstreamRemote -and $upstreamRemote -ne 'origin') {
        Write-Warning "当前分支拉取远端是 $upstreamRemote，不是 origin。拉取可能会走错远端：$repoName"
    }

    $dirty = & git -C $repoPath status --porcelain
    if ($LASTEXITCODE -eq 0 -and $dirty) {
        Write-Warning "工作区有未提交改动，这些改动不会被推送：$repoName"
    }

    $pushArgs = @('push', 'gitee', "refs/heads/$($branch):refs/heads/$($branch)")
    if ($ForceWithLease) {
        $pushArgs += '--force-with-lease'
    }

    if ($DryRun) {
        Write-Host ("DRY-RUN  git -C `"{0}`" {1}" -f $repoPath, ($pushArgs -join ' ')) -ForegroundColor DarkGray
    }
    else {
        Write-Host "推送 $repoName [$branch] -> gitee" -ForegroundColor Green
        Invoke-Git -RepoPath $repoPath -GitArgs $pushArgs
    }

    if ($Tags) {
        $tagArgs = @('push', 'gitee', '--tags')
        if ($DryRun) {
            Write-Host ("DRY-RUN  git -C `"{0}`" {1}" -f $repoPath, ($tagArgs -join ' ')) -ForegroundColor DarkGray
        }
        else {
            Write-Host "推送 $repoName tags -> gitee" -ForegroundColor Green
            Invoke-Git -RepoPath $repoPath -GitArgs $tagArgs
        }
    }
}

Write-Host "`n全部完成。" -ForegroundColor Green
