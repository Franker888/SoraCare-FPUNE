$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$archivo = Join-Path $proyecto '.env.adri-admin-railway.clixml'
if (-not (Test-Path -LiteralPath $archivo)) {
    throw 'Todavia no existe la credencial cifrada de Adri Admin.'
}

$registro = Import-Clixml -LiteralPath $archivo
if (-not $registro -or -not $registro.Contrasena) {
    throw 'La credencial cifrada de Adri Admin no es valida.'
}

$credencial = [Management.Automation.PSCredential]::new(
    $registro.Usuario,
    $registro.Contrasena
)
$texto = $null
try {
    $texto = $credencial.GetNetworkCredential().Password
    Set-Clipboard -Value $texto
} finally {
    $texto = $null
}

Write-Host ("Usuario: {0}" -f $registro.Usuario)
Write-Host 'La contrasena fue copiada al portapapeles sin mostrarla.'
