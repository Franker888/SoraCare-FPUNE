$ErrorActionPreference = 'Stop'

Write-Host 'Configuracion local del monitoreo con Sentry' -ForegroundColor Cyan
Write-Host 'El DSN se guardara como variable de entorno de tu usuario de Windows.'
Write-Host 'No se escribira dentro del proyecto ni se mostrara en pantalla.'

$seguro = Read-Host 'Pega el DSN de Sentry' -AsSecureString
$punteroSentry = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)

try {
    $dsnSentry = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($punteroSentry)
    if ([string]::IsNullOrWhiteSpace($dsnSentry) -or
        -not $dsnSentry.StartsWith('https://')) {
        throw 'El DSN de Sentry no tiene un formato valido.'
    }

    [Environment]::SetEnvironmentVariable(
        'SORACARE_SENTRY_DSN',
        $dsnSentry.Trim(),
        'User'
    )
    [Environment]::SetEnvironmentVariable(
        'SORACARE_SENTRY_ENVIRONMENT',
        'local',
        'User'
    )
    Write-Host 'Sentry quedo configurado para SoraCare.' -ForegroundColor Green
    Write-Host 'Reinicia SoraCare con scripts\iniciar-local-etapa8.ps1 para aplicar el cambio.' -ForegroundColor Yellow
} finally {
    if ($punteroSentry -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($punteroSentry)
    }
    $dsnSentry = $null
    $seguro = $null
}
