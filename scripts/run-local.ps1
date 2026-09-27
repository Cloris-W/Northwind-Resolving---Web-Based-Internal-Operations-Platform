[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

function Get-RepositoryRoot {
    return (Split-Path -Parent $PSScriptRoot)
}

function Import-LocalEnvironment([string]$RepositoryRoot) {
    $environmentFile = Join-Path $RepositoryRoot '.env.local'
    if (-not (Test-Path -LiteralPath $environmentFile -PathType Leaf)) { return }

    foreach ($line in Get-Content -LiteralPath $environmentFile) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) { throw "Invalid .env.local entry: $line" }
        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1)
        Set-Item -Path "Env:$name" -Value $value
    }

}

$repositoryRoot = Get-RepositoryRoot
Import-LocalEnvironment $repositoryRoot
$defaults = @{ POSTGRES_HOST = 'localhost'; POSTGRES_PORT = '5432'; POSTGRES_DB = 'northwind_resolve'; POSTGRES_USER = 'northwind'; POSTGRES_PASSWORD = 'change-me-for-local-development' }
foreach ($name in $defaults.Keys) { if ([string]::IsNullOrWhiteSpace((Get-Item -Path "Env:$name" -ErrorAction SilentlyContinue).Value)) { Set-Item -Path "Env:$name" -Value $defaults[$name] } }

Push-Location $repositoryRoot
try {
    & docker compose up -d
    if ($LASTEXITCODE -ne 0) { throw 'docker compose up -d failed.' }

    $env:SPRING_PROFILES_ACTIVE = 'database'
    $env:SERVER_PORT = '18080'
    Push-Location (Join-Path $repositoryRoot 'backend')
    try {
        # PostgreSQL credentials remain in process environment and are never written to output.
        & .\gradlew.bat bootRun
    }
    finally {
        Pop-Location
    }
}
finally {
    Pop-Location
}
