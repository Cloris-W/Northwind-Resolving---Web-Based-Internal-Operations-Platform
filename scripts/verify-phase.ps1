[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$backendProcess = $null
$backendLog = $null
$backendErrorLog = $null

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
        Set-Item -Path "Env:$($trimmed.Substring(0, $separator).Trim())" -Value $trimmed.Substring($separator + 1)
    }

}

function Invoke-Checked([string]$Description, [scriptblock]$Action) {
    Write-Host "`n==> $Description"
    & $Action
    if ($LASTEXITCODE -ne 0) { throw "$Description failed with exit code $LASTEXITCODE." }
}

function Get-BackendTestSummary([string]$BackendDirectory) {
    $resultDirectory = Join-Path $BackendDirectory 'build/test-results/test'
    $files = Get-ChildItem -Path $resultDirectory -Filter 'TEST-*.xml' -File -ErrorAction Stop
    if ($files.Count -eq 0) { throw 'Backend tests produced no JUnit XML results.' }

    $summary = [ordered]@{ Tests = 0; Failures = 0; Errors = 0; Skipped = 0 }
    foreach ($file in $files) {
        [xml]$xml = Get-Content -LiteralPath $file.FullName
        $suite = $xml.testsuite
        $summary.Tests += [int]$suite.tests
        $summary.Failures += [int]$suite.failures
        $summary.Errors += [int]$suite.errors
        $summary.Skipped += [int]$suite.skipped
    }
    return [pscustomobject]$summary
}

function Wait-ForEndpoint([string]$Url) {
    $deadline = [DateTime]::UtcNow.AddSeconds(90)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) { return $response.StatusCode }
        }
        catch { Start-Sleep -Seconds 2 }
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Timed out waiting for $Url to return HTTP 200. Logs: $backendLog ; $backendErrorLog"
}

function Assert-NoTrackedGeneratedDirectories([string]$RepositoryRoot) {
    $patterns = @(
        'frontend/node_modules/**', 'frontend/dist/**', 'frontend/.angular/**',
        'backend/build/**', 'backend/.gradle/**', 'backend/.kotlin/**', 'backend/out/**', 'backend/bin/**'
    )
    $tracked = & git -c "safe.directory=$RepositoryRoot" ls-files -- $patterns
    if ($LASTEXITCODE -ne 0) { throw 'Unable to inspect tracked generated directories.' }
    if ($tracked) { throw "Generated files are tracked by Git:`n$($tracked -join "`n")" }
}

$repositoryRoot = Get-RepositoryRoot
$backendDirectory = Join-Path $repositoryRoot 'backend'
$frontendDirectory = Join-Path $repositoryRoot 'frontend'

try {
    Import-LocalEnvironment $repositoryRoot
    $defaults = @{ POSTGRES_HOST = 'localhost'; POSTGRES_PORT = '5432'; POSTGRES_DB = 'northwind_resolve'; POSTGRES_USER = 'northwind'; POSTGRES_PASSWORD = 'change-me-for-local-development' }
    foreach ($name in $defaults.Keys) { if ([string]::IsNullOrWhiteSpace((Get-Item -Path "Env:$name" -ErrorAction SilentlyContinue).Value)) { Set-Item -Path "Env:$name" -Value $defaults[$name] } }

    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI is not installed or is not on PATH.' }
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Docker is not available. Start Docker Desktop and rerun the phase gate.' }

    Push-Location $repositoryRoot
    try { Invoke-Checked 'Start local Docker services' { & docker compose up -d } }
    finally { Pop-Location }

    Push-Location $backendDirectory
    try { Invoke-Checked 'Run all backend tests with Testcontainers' { & .\gradlew.bat test } }
    finally { Pop-Location }

    $backendTests = Get-BackendTestSummary $backendDirectory
    Write-Host "Backend tests: $($backendTests.Tests) total; $($backendTests.Failures) failures; $($backendTests.Errors) errors; $($backendTests.Skipped) skipped"
    if ($backendTests.Failures -ne 0 -or $backendTests.Errors -ne 0 -or $backendTests.Skipped -ne 0) {
        throw 'Backend test gate requires zero failures, errors, and skipped tests.'
    }

    Push-Location $backendDirectory
    try { Invoke-Checked 'Build backend' { & .\gradlew.bat build } }
    finally { Pop-Location }

    Push-Location $frontendDirectory
    try {
        Invoke-Checked 'Run all frontend tests' { & npm.cmd test }
        Invoke-Checked 'Build frontend production bundle' { & npm.cmd run build }
    }
    finally { Pop-Location }

    $env:SPRING_PROFILES_ACTIVE = 'database'
    $env:SERVER_PORT = '18080'
    $backendLog = Join-Path $backendDirectory 'build/verify-phase-backend.out.log'
    $backendErrorLog = Join-Path $backendDirectory 'build/verify-phase-backend.err.log'
    Remove-Item -LiteralPath $backendLog, $backendErrorLog -Force -ErrorAction SilentlyContinue
    $backendProcess = Start-Process -FilePath (Join-Path $backendDirectory 'gradlew.bat') -ArgumentList 'bootRun' -WorkingDirectory $backendDirectory -RedirectStandardOutput $backendLog -RedirectStandardError $backendErrorLog -PassThru

    $health = Wait-ForEndpoint 'http://localhost:18080/actuator/health'
    $contract = Wait-ForEndpoint 'http://localhost:18080/api-contract.yaml'
    $swagger = Wait-ForEndpoint 'http://localhost:18080/swagger-ui/index.html'
    Write-Host "Endpoint checks: health=$health; OpenAPI=$contract; Swagger=$swagger"

    Invoke-Checked 'Check Git diff whitespace' { & git -c "safe.directory=$repositoryRoot" diff --check }
    Assert-NoTrackedGeneratedDirectories $repositoryRoot

    Write-Host "`nPHASE GATE: PASS"
}
catch {
    Write-Error "`nPHASE GATE: FAIL - $($_.Exception.Message)"
    exit 1
}
finally {
    if ($null -ne $backendProcess -and -not $backendProcess.HasExited) {
        & taskkill.exe /PID $backendProcess.Id /T /F *> $null
    }
}
