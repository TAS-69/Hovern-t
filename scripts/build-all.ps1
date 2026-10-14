param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string[]]$Versions = @(),
    [ValidateSet('fabric', 'neoforge')][string[]]$Platforms = @('fabric', 'neoforge')
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
$matrix = Get-Content -LiteralPath 'versions.json' -Raw | ConvertFrom-Json
if (!$Versions.Count) { $Versions = $matrix.PSObject.Properties.Name }
$release = ((Get-Content -LiteralPath 'gradle.properties' | Where-Object { $_ -match '^version=' }) -replace '^version=', '').Trim()
$destination = Join-Path $projectRoot "dist\$release"
New-Item -ItemType Directory -Force -Path (Join-Path $destination 'logs') | Out-Null
foreach ($minecraft in $Versions) {
    if ($matrix.PSObject.Properties.Name -notcontains $minecraft) { throw "Unsupported build target: $minecraft" }
    foreach ($platform in $Platforms) {
        $log = Join-Path $destination "logs\$platform-$minecraft.log"
        Write-Host "Building Hovern't for $minecraft ($platform)..."
        & .\gradlew.bat "-Pmc=$minecraft" "-Pplatform=$platform" 'collectJars' '--console=plain' *> $log
        if ($LASTEXITCODE -ne 0) { throw "Build failed; see $log" }
    }
}
Get-ChildItem -LiteralPath $destination -Filter '*.jar' | Get-FileHash -Algorithm SHA256 | ForEach-Object {
    "$($_.Hash.ToLower())  $(Split-Path -Leaf $_.Path)"
} | Set-Content -LiteralPath (Join-Path $destination 'SHA256SUMS.txt') -Encoding ascii
Write-Host "Installable files: $destination"
