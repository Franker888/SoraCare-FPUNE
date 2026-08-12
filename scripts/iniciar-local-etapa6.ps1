$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$war = Join-Path $proyecto 'target\SoraCare-FPUNE.war'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$tomcat = 'C:\Users\adria\Desktop\Hospital\Servidor\apache-tomcat-10.1.57'
$javaHomeEtapa6 = 'C:\Users\adria\.jdks\ms-21.0.12'

foreach ($ruta in @($war, $psql, $tomcat, $javaHomeEtapa6)) {
    if (-not (Test-Path -LiteralPath $ruta)) {
        throw "No se encontro el recurso requerido: $ruta"
    }
}

$segura = Read-Host 'Contrasena de PostgreSQL para soracare_app' -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)

try {
    $dbPasswordEtapa6 = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    $env:PGPASSWORD = $dbPasswordEtapa6
    $env:SORACARE_DB_PASSWORD = $dbPasswordEtapa6

    & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c 'SELECT 1;' | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible conectarse a PostgreSQL con la contrasena ingresada.'
    }

    $destinoWar = Join-Path $tomcat 'webapps\SoraCare-FPUNE.war'
    Copy-Item -LiteralPath $war -Destination $destinoWar -Force

    $shutdown = Join-Path $tomcat 'bin\shutdown.bat'
    $startup = Join-Path $tomcat 'bin\startup.bat'
    $errorActionAnterior = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $shutdown *> $null
        $codigoShutdown = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $errorActionAnterior
    }

    if ($codigoShutdown -eq 0) {
        Start-Sleep -Seconds 2
    } else {
        Write-Host 'Tomcat ya estaba detenido; se continuara con el inicio.' `
            -ForegroundColor Yellow
    }

    $env:JAVA_HOME = $javaHomeEtapa6
    $env:CATALINA_HOME = $tomcat
    Start-Process -FilePath $startup -WorkingDirectory (Join-Path $tomcat 'bin') `
        -WindowStyle Hidden -Wait

    Write-Host 'Etapa 6 desplegada. Tomcat fue iniciado en http://localhost:8080/SoraCare-FPUNE/' `
        -ForegroundColor Green
} finally {
    if ($puntero -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    }
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_DB_PASSWORD -ErrorAction SilentlyContinue
    $dbPasswordEtapa6 = $null
    $segura = $null
}
