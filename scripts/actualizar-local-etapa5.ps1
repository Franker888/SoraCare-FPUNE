$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$migracion = Join-Path $proyecto 'src\main\resources\db\migration\V018__completar_etapa_pagos.sql'
$war = Join-Path $proyecto 'target\SoraCare-FPUNE.war'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$tomcat = 'C:\Users\adria\Desktop\Hospital\Servidor\apache-tomcat-10.1.57'
$javaHomeEtapa5 = 'C:\Users\adria\.jdks\ms-21.0.12'

foreach ($ruta in @($migracion, $war, $psql, $tomcat, $javaHomeEtapa5)) {
    if (-not (Test-Path -LiteralPath $ruta)) {
        throw "No se encontro el recurso requerido: $ruta"
    }
}

$segura = Read-Host 'Contrasena de PostgreSQL para soracare_app' -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)

try {
    $dbPasswordEtapa5 = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    $env:PGPASSWORD = $dbPasswordEtapa5
    $env:SORACARE_DB_PASSWORD = $dbPasswordEtapa5

    $tablaExiste = & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c "SELECT to_regclass('soracare.datos_facturacion_pago') IS NOT NULL;"
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible conectarse a PostgreSQL con la contrasena ingresada.'
    }

    if (($tablaExiste | Out-String).Trim() -eq 't') {
        Write-Host 'La migracion de la etapa 5 ya estaba aplicada.' -ForegroundColor Yellow
    } else {
        & $psql -w -h localhost -U soracare_app -d soracare_db `
            -v ON_ERROR_STOP=1 -f $migracion
        if ($LASTEXITCODE -ne 0) {
            throw 'La migracion de la etapa 5 no pudo completarse.'
        }
        Write-Host 'Migracion de la etapa 5 aplicada.' -ForegroundColor Green
    }

    $destinoWar = Join-Path $tomcat 'webapps\SoraCare-FPUNE.war'
    Copy-Item -LiteralPath $war -Destination $destinoWar -Force

    $shutdown = Join-Path $tomcat 'bin\shutdown.bat'
    $startup = Join-Path $tomcat 'bin\startup.bat'
    & $shutdown 2>$null | Out-Null
    Start-Sleep -Seconds 2

    $env:JAVA_HOME = $javaHomeEtapa5
    $env:CATALINA_HOME = $tomcat
    Start-Process -FilePath $startup -WorkingDirectory (Join-Path $tomcat 'bin') `
        -WindowStyle Hidden -Wait

    Write-Host 'Etapa 5 desplegada. Tomcat fue iniciado en http://localhost:8080/SoraCare-FPUNE/' -ForegroundColor Green
} finally {
    if ($puntero -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    }
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_DB_PASSWORD -ErrorAction SilentlyContinue
    $dbPasswordEtapa5 = $null
    $segura = $null
}
