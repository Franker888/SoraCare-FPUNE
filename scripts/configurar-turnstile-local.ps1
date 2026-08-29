$ErrorActionPreference = 'Stop'

Write-Host 'Configuracion local de Cloudflare Turnstile' -ForegroundColor Cyan
Write-Host 'La clave publica y el secreto se guardaran como variables de entorno del usuario de Windows.'
Write-Host 'El secreto no se escribira dentro del proyecto ni se mostrara en pantalla.'

$siteKey = Read-Host 'Pega la Site Key de Turnstile'
$secretKeySegura = Read-Host 'Pega la Secret Key de Turnstile' -AsSecureString
$punteroSecretKey = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secretKeySegura)

try {
    $secretKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($punteroSecretKey)
    if ([string]::IsNullOrWhiteSpace($siteKey) -or $siteKey.Trim().Length -lt 20) {
        throw 'La Site Key ingresada no posee un formato valido.'
    }
    if ([string]::IsNullOrWhiteSpace($secretKey) -or $secretKey.Length -lt 20) {
        throw 'La Secret Key ingresada no posee un formato valido.'
    }

    [Environment]::SetEnvironmentVariable(
        'SORACARE_TURNSTILE_SITE_KEY',
        $siteKey.Trim(),
        'User'
    )
    [Environment]::SetEnvironmentVariable(
        'SORACARE_TURNSTILE_SECRET_KEY',
        $secretKey,
        'User'
    )

    Write-Host 'Cloudflare Turnstile quedo configurado para SoraCare.' -ForegroundColor Green
    Write-Host 'Reinicia SoraCare con scripts\iniciar-local-etapa8.ps1 para aplicar el cambio.' -ForegroundColor Green
} finally {
    if ($punteroSecretKey -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($punteroSecretKey)
    }
    $secretKey = $null
    $secretKeySegura = $null
}
