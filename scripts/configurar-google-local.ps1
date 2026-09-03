param(
    [ValidateRange(1, 65535)]
    [int]$Puerto = 8081
)

$ErrorActionPreference = 'Stop'

$redirectUri = "http://localhost:$Puerto/SoraCare-FPUNE/google/calendar/callback"

Write-Host 'Configuracion local de Google Identity y Google Calendar' -ForegroundColor Cyan
Write-Host 'Las credenciales se guardaran como variables de tu usuario de Windows.'
Write-Host 'No se escribiran dentro del proyecto ni se mostrara el secreto en pantalla.'
Write-Host "Redirect URI que debes registrar en Google Cloud: $redirectUri"

$clientId = (Read-Host 'Pega el Client ID OAuth de Google').Trim()
if (-not $clientId.EndsWith('.apps.googleusercontent.com', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'El Client ID no posee el formato esperado de Google OAuth.'
}

$secretSeguro = Read-Host 'Pega el Client Secret OAuth de Google' -AsSecureString
$punteroSecret = [IntPtr]::Zero
$secretPlano = $null
$claveGenerada = $null
$claveExistenteBytes = $null

try {
    $punteroSecret = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secretSeguro)
    $secretPlano = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($punteroSecret)
    if ([string]::IsNullOrWhiteSpace($secretPlano) -or $secretPlano.Length -lt 8) {
        throw 'El Client Secret ingresado no posee un formato valido.'
    }

    $claveCifrado = [Environment]::GetEnvironmentVariable(
        'SORACARE_GOOGLE_TOKEN_ENCRYPTION_KEY',
        'User'
    )
    if ([string]::IsNullOrWhiteSpace($claveCifrado)) {
        $bytes = New-Object byte[] 32
        $generador = [Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $generador.GetBytes($bytes)
            $claveGenerada = [Convert]::ToBase64String($bytes)
            $claveCifrado = $claveGenerada
        } finally {
            $generador.Dispose()
            [Array]::Clear($bytes, 0, $bytes.Length)
        }
    } else {
        try {
            $claveExistenteBytes = [Convert]::FromBase64String($claveCifrado)
        } catch {
            throw 'La clave de cifrado existente no es Base64 valida y no fue reemplazada.'
        }
        if ($claveExistenteBytes.Length -ne 32) {
            throw 'La clave de cifrado existente no tiene 32 bytes y no fue reemplazada.'
        }
    }

    [Environment]::SetEnvironmentVariable(
        'SORACARE_GOOGLE_CLIENT_ID',
        $clientId,
        'User'
    )
    [Environment]::SetEnvironmentVariable(
        'SORACARE_GOOGLE_CLIENT_SECRET',
        $secretPlano,
        'User'
    )
    [Environment]::SetEnvironmentVariable(
        'SORACARE_GOOGLE_CALENDAR_REDIRECT_URI',
        $redirectUri,
        'User'
    )
    [Environment]::SetEnvironmentVariable(
        'SORACARE_GOOGLE_TOKEN_ENCRYPTION_KEY',
        $claveCifrado,
        'User'
    )

    Write-Host 'Google Identity y Google Calendar quedaron configurados.' -ForegroundColor Green
    Write-Host 'Reinicia SoraCare con scripts\iniciar-docker.ps1 para aplicar el cambio.' -ForegroundColor Green
} finally {
    if ($punteroSecret -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($punteroSecret)
    }
    if ($claveExistenteBytes) {
        [Array]::Clear($claveExistenteBytes, 0, $claveExistenteBytes.Length)
    }
    $secretPlano = $null
    $secretSeguro = $null
    $claveGenerada = $null
}
