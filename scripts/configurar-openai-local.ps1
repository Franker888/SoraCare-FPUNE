$ErrorActionPreference = 'Stop'

Write-Host 'Configuracion local de Sora con OpenAI' -ForegroundColor Cyan
Write-Host 'La clave se guardara como variable de entorno de tu usuario de Windows.'
Write-Host 'No se escribira dentro del proyecto ni se mostrara en pantalla.'

$claveSegura = Read-Host 'Pega tu clave secreta de OpenAI' -AsSecureString
$punteroClave = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($claveSegura)

try {
    $clavePlana = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($punteroClave)
    if ([string]::IsNullOrWhiteSpace($clavePlana) -or $clavePlana.Length -lt 20) {
        throw 'La clave ingresada no posee un formato valido.'
    }

    [Environment]::SetEnvironmentVariable(
        'SORACARE_OPENAI_API_KEY',
        $clavePlana,
        'User'
    )
    [Environment]::SetEnvironmentVariable(
        'SORACARE_OPENAI_MODEL',
        'gpt-5.6-terra',
        'User'
    )

    Write-Host 'OpenAI quedo configurado para Sora.' -ForegroundColor Green
    Write-Host 'Reinicia SoraCare con scripts\iniciar-local-etapa8.ps1 para aplicar el cambio.' -ForegroundColor Green
} finally {
    if ($punteroClave -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($punteroClave)
    }
    $clavePlana = $null
    $claveSegura = $null
}
