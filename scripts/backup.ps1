# HazeRoot 本地快照备份脚本
# 用途：定时/手动打包源码 + 文档 + APK 产物，防本地误删。
# 用法：powershell -ExecutionPolicy Bypass -File backup.ps1 [-Keep 20]
param(
    [int]$Keep = 20
)

$ErrorActionPreference = "Stop"
$proj  = "D:\WorkBuddy\RootScriptRunner"
$out   = "D:\WorkBuddy\backup-auto"
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$zip   = Join-Path $out "HazeRoot-$stamp.zip"

if (-not (Test-Path $out)) { New-Item -ItemType Directory -Path $out | Out-Null }

# 打包源码（排除构建产物）
$srcStage = Join-Path $env:TEMP "hazeroot-src-$stamp"
if (Test-Path $srcStage) { Remove-Item $srcStage -Recurse -Force }
New-Item -ItemType Directory -Path $srcStage | Out-Null

Copy-Item "$proj\app"           $srcStage -Recurse
Copy-Item "$proj\gradle"        $srcStage -Recurse
Copy-Item "$proj\docs"          $srcStage -Recurse
Copy-Item "$proj\recovery"      $srcStage -Recurse
Copy-Item "$proj\.gitignore"    $srcStage
Copy-Item "$proj\build.gradle.kts" $srcStage
Copy-Item "$proj\gradle.properties" $srcStage
Copy-Item "$proj\gradlew"       $srcStage
Copy-Item "$proj\gradlew.bat"   $srcStage
Copy-Item "$proj\LICENSE"       $srcStage
Copy-Item "$proj\README.md"     $srcStage
Copy-Item "$proj\settings.gradle.kts" $srcStage
Copy-Item "$proj\local.properties" $srcStage -ErrorAction SilentlyContinue

# 清理 build 产物
Get-ChildItem $srcStage -Recurse -Directory -Include "build",".gradle" |
    Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

# 打包
Compress-Archive -Path "$srcStage\*" -DestinationPath $zip -Force
Remove-Item $srcStage -Recurse -Force

# 清理旧快照，保留最近 Keep 份
$all = Get-ChildItem $out -Filter "HazeRoot-*.zip" | Sort-Object LastWriteTime -Descending
if ($all.Count -gt $Keep) {
    $all | Select-Object -Skip $Keep | Remove-Item -Force
}

Write-Output "快照已生成：$zip"
Write-Output ("当前保留 {0} 份（上限 {1} 份）" -f (Get-ChildItem $out -Filter "HazeRoot-*.zip").Count, $Keep)
