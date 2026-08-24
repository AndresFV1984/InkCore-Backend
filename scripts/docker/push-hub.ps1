# Publica imágenes InkCore en Docker Hub (el compose del cliente solo hace pull).
# Tag de versión = v + día.mes (ej. v22.07) + latest.
#
#   bayronindicore/inkcore-backend
#   bayronindicore/inkcore-minio-init
#   bayronindicore/inkcore-postgres
#
# Uso:
#   .\scripts\docker\push-hub.ps1
#   .\scripts\docker\push-hub.ps1 -SkipPackage   # JAR ya en target/
#   .\scripts\docker\push-hub.ps1 -SkipBackend   # solo init + postgres

param(
    [switch]$SkipPackage,
    [switch]$SkipBackend,
    [string]$Namespace = "bayronindicore"
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

function Push-TaggedImage {
    param([string]$Image)
    Write-Host "Push ${Image}:$script:tag ..."
    & docker push "${Image}:$script:tag"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "Push ${Image}:latest ..."
    & docker push "${Image}:latest"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$tag = Get-Date -Format "vdd.MM"
$script:tag = $tag
Write-Host "Tag de versión: $tag"

if (-not $SkipBackend) {
    if (-not $SkipPackage) {
        Write-Host "Compilando JAR..."
        & .\mvnw.cmd "-Dmaven.test.skip=true" package
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }

    $jar = Get-ChildItem -Path "target\inkcore-backend-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } |
        Select-Object -First 1
    if (-not $jar) {
        Write-Error "No se encontró target\inkcore-backend-*.jar. Ejecuta el package primero."
    }

    $backend = "$Namespace/inkcore-backend"
    Write-Host "Construyendo imagen ${backend}:$tag ..."
    & docker build --provenance=false --sbom=false `
        -t "${backend}:${tag}" `
        -t "${backend}:latest" `
        .
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Push-TaggedImage $backend
}

$minioInit = "$Namespace/inkcore-minio-init"
Write-Host "Construyendo imagen ${minioInit}:$tag ..."
& docker build --provenance=false --sbom=false `
    -t "${minioInit}:${tag}" `
    -t "${minioInit}:latest" `
    ".\scripts\minio"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Push-TaggedImage $minioInit

$postgres = "$Namespace/inkcore-postgres"
Write-Host "Construyendo imagen ${postgres}:$tag ..."
& docker build --provenance=false --sbom=false `
    -t "${postgres}:${tag}" `
    -t "${postgres}:latest" `
    ".\scripts\postgres"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Push-TaggedImage $postgres

Write-Host "Listo: ${Namespace}/inkcore-backend, inkcore-minio-init, inkcore-postgres  ($tag + latest)"
