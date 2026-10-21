$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

function Find-Java {
    $command = Get-Command java -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }

    $patterns = @(
        "$env:LOCALAPPDATA\Programs\Microsoft\jdk-*\bin\java.exe",
        "C:\Program Files\Microsoft\jdk-*\bin\java.exe",
        "C:\Program Files\Eclipse Adoptium\jdk-*\bin\java.exe"
    )
    foreach ($pattern in $patterns) {
        $found = Get-ChildItem $pattern -ErrorAction SilentlyContinue | Sort-Object FullName -Descending | Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $null
}

$java = Find-Java
if (-not $java) {
    Write-Host "未找到 Java 17+。请先安装 JDK 17（推荐 Eclipse Temurin 或 Microsoft OpenJDK）。" -ForegroundColor Red
    Read-Host "按 Enter 退出"
    exit 1
}

$jar = Join-Path $PSScriptRoot "dist\crop-agent.jar"
if (-not (Test-Path $jar)) { $jar = Join-Path $PSScriptRoot "target\crop-agent.jar" }
if (-not (Test-Path $jar)) {
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvn) {
        Write-Host "未找到已构建 JAR，也未找到 Maven。请执行 mvn -DskipTests package。" -ForegroundColor Red
        Read-Host "按 Enter 退出"
        exit 1
    }
    Write-Host "正在构建项目..." -ForegroundColor Cyan
    & $mvn.Source -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Maven 构建失败" }
}

Write-Host "禾诊智能体正在启动，请稍后打开：http://localhost:8080" -ForegroundColor Green
Write-Host "按 Ctrl+C 可停止服务。"
& $java -jar $jar

