# Corrige el error 42501 "permiso denegado al esquema indicolors" para indicore_app.
# Requiere la contraseña de un rol con privilegios (por defecto indicore_admin).
param(
    [string]$Host = "localhost",
    [int]$Port = 5432,
    [string]$Database = "indicore",
    [string]$AdminUser = "indicore_admin",
    [string]$AppUser = "indicore_app"
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDir "grant-indicore-app-indicolors.sql"

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    Write-Error "psql no está en el PATH. Instala el cliente PostgreSQL o añade su bin al PATH."
}

$secure = Read-Host "Contraseña de '$AdminUser'" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}

Write-Host "Aplicando permisos en ${Database}@${Host}:${Port} ..."
& psql -h $Host -p $Port -U $AdminUser -d $Database -f $sqlFile
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Listo. Reinicia la aplicación (ya no hace falta FLYWAY_USER si los GRANT quedaron aplicados)."
