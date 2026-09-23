param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('medicos', 'administracion', 'recepcion', 'paciente')]
    [string]$Cuenta
)

$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$archivo = Join-Path $proyecto '.env.demo-railway.clixml'
if (-not (Test-Path -LiteralPath $archivo)) {
    throw 'No existen credenciales demostrativas cifradas en este equipo.'
}

$registro = Import-Clixml -LiteralPath $archivo |
    Where-Object Clave -EQ $Cuenta |
    Select-Object -First 1
if (-not $registro -or -not $registro.Contrasena) {
    throw "No se encontró la cuenta demostrativa $Cuenta."
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
Write-Host 'La contraseña fue copiada al portapapeles y no se mostró en pantalla.'
