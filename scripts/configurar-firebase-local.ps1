$ErrorActionPreference = 'Stop'

Write-Host 'Configuracion local de Firebase Cloud Messaging' -ForegroundColor Cyan
Write-Host 'Los valores se guardaran como variables de entorno de tu usuario de Windows.'
Write-Host 'No se escribiran dentro del proyecto ni se mostraran en pantalla.'

function Leer-ValorSeguro([string]$mensaje) {
    $seguro = Read-Host $mensaje -AsSecureString
    $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    } finally {
        if ($puntero -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
        }
    }
}

$apiKey = Leer-ValorSeguro 'Pega la Web API Key de Firebase'
$appId = Leer-ValorSeguro 'Pega el App ID de SoraCare-Web'
$vapidKey = Leer-ValorSeguro 'Pega la clave publica VAPID que acabas de generar'

if ([string]::IsNullOrWhiteSpace($apiKey) -or
    [string]::IsNullOrWhiteSpace($appId) -or
    [string]::IsNullOrWhiteSpace($vapidKey)) {
    throw 'Los tres valores son obligatorios para activar Firebase Cloud Messaging.'
}

[Environment]::SetEnvironmentVariable('SORACARE_FIREBASE_API_KEY', $apiKey.Trim(), 'User')
[Environment]::SetEnvironmentVariable('SORACARE_FIREBASE_AUTH_DOMAIN', 'soracare-fpune.firebaseapp.com', 'User')
[Environment]::SetEnvironmentVariable('SORACARE_FIREBASE_PROJECT_ID', 'soracare-fpune', 'User')
[Environment]::SetEnvironmentVariable('SORACARE_FIREBASE_STORAGE_BUCKET', 'soracare-fpune.firebasestorage.app', 'User')
[Environment]::SetEnvironmentVariable('SORACARE_FIREBASE_MESSAGING_SENDER_ID', '814530755918', 'User')
[Environment]::SetEnvironmentVariable('SORACARE_FIREBASE_APP_ID', $appId.Trim(), 'User')
[Environment]::SetEnvironmentVariable('SORACARE_FIREBASE_VAPID_KEY', $vapidKey.Trim(), 'User')

$apiKey = $null
$appId = $null
$vapidKey = $null

Write-Host 'Firebase Cloud Messaging quedo configurado para SoraCare.' -ForegroundColor Green
Write-Host 'Reinicia SoraCare con scripts\iniciar-local-etapa8.ps1 para aplicar el cambio.'
