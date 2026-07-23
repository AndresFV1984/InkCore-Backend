# Publica bayronindicore/inkcore-backend en Docker Hub.
# Tag de versión = v + día.mes (ej. v22.07) + latest.
#
# Uso:
#   .\scripts\docker\push-hub.ps1
#   .\scripts\docker\push-hub.ps1 -SkipPackage   # si el JAR ya está en target/

param(
    [switch]$SkipPackage,
    [string]$Image = "bayronindicore/inkcore-backend"
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

$tag = Get-Date -Format "vdd.MM"
Write-Host "Tag de versión: $tag"

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

Write-Host "Construyendo imagen $Image`:$tag ..."
# Sin provenance/SBOM: compose/buildx genera attestations que rompen el push
# ("does not provide any platform") en Docker Hub.
& docker build --provenance=false --sbom=false `
    -t "${Image}:${tag}" `
    -t "${Image}:latest" `
    .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Push $Image`:$tag ..."
& docker push "${Image}:${tag}"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Push $Image`:latest ..."
& docker push "${Image}:latest"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Listo: ${Image}:${tag} y ${Image}:latest"
