$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$dbPath = Join-Path $repoRoot "app/src/main/assets/colors.db"
$jsonPath = Join-Path $repoRoot "app/src/main/assets/colors/colors.json"
$sqlPath = Join-Path $repoRoot "app/src/main/assets/colors_import.sql"
$sqlite = "C:\Users\eugen\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe"

if (-not (Test-Path $sqlite)) {
    throw "sqlite3 not found at: $sqlite"
}
if (-not (Test-Path $jsonPath)) {
    throw "Missing source JSON: $jsonPath"
}

if (Test-Path $dbPath) { Remove-Item $dbPath -Force }
if (Test-Path $sqlPath) { Remove-Item $sqlPath -Force }

$colors = Get-Content $jsonPath -Raw | ConvertFrom-Json

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('BEGIN TRANSACTION;')
$lines.Add('CREATE TABLE IF NOT EXISTS "color_catalog_entries" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "catalogId" TEXT NOT NULL, "languageTag" TEXT NOT NULL, "name" TEXT NOT NULL, "hex" TEXT NOT NULL);')
$lines.Add('CREATE UNIQUE INDEX IF NOT EXISTS "index_color_catalog_entries_catalogId_languageTag_name" ON "color_catalog_entries" ("catalogId", "languageTag", "name");')
$lines.Add('CREATE INDEX IF NOT EXISTS "index_color_catalog_entries_catalogId_hex" ON "color_catalog_entries" ("catalogId", "hex");')

foreach ($c in $colors) {
    $name = ([string]$c.name).Replace("'", "''")
    $hex = ([string]$c.hex).Replace("'", "''")
    $lines.Add(('INSERT INTO "color_catalog_entries" ("catalogId","languageTag","name","hex") VALUES (''default'',''en'',''{0}'',''{1}'');' -f $name, $hex))
}

$lines.Add('COMMIT;')
$lines | Set-Content -Encoding UTF8 $sqlPath

& $sqlite $dbPath ".read $sqlPath" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "sqlite import failed"
}

$dbCount = & $sqlite $dbPath "SELECT COUNT(*) FROM color_catalog_entries WHERE catalogId='default' AND languageTag='en';"
$jsonCount = $colors.Count

Remove-Item $sqlPath -Force

Write-Output "Generated $dbPath"
Write-Output "Rows inserted: $dbCount (from colors.json entries: $jsonCount)"
