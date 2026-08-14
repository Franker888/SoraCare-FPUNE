$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$migracionPerfilClinico = Join-Path $proyecto 'src\main\resources\db\migration\V019__agregar_perfil_clinico_paciente.sql'
$migracionSignosVitales = Join-Path $proyecto 'src\main\resources\db\migration\V020__agregar_signos_vitales_atencion.sql'
$migracionAreaAfectada = Join-Path $proyecto 'src\main\resources\db\migration\V021__agregar_evaluacion_area_afectada.sql'
$war = Join-Path $proyecto 'target\SoraCare-FPUNE.war'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$tomcat = 'C:\Users\adria\Desktop\Hospital\Servidor\apache-tomcat-10.1.57'
$javaHomeEtapa6 = 'C:\Users\adria\.jdks\ms-21.0.12'
$directorioDocumentos = 'C:\Users\adria\Desktop\Hospital\Datos\SoraCare\documentos-clinicos'
$directorioDocumentosAnterior = Join-Path ([Environment]::GetFolderPath('UserProfile')) `
    '.soracare\documentos-clinicos'

foreach ($ruta in @(
    $migracionPerfilClinico,
    $migracionSignosVitales,
    $migracionAreaAfectada,
    $war,
    $psql,
    $tomcat,
    $javaHomeEtapa6
)) {
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
    $env:SORACARE_DOCUMENTOS_DIR = $directorioDocumentos

    New-Item -ItemType Directory -Path $directorioDocumentos -Force | Out-Null
    if (Test-Path -LiteralPath $directorioDocumentosAnterior) {
        Get-ChildItem -LiteralPath $directorioDocumentosAnterior | ForEach-Object {
            Copy-Item -LiteralPath $_.FullName -Destination $directorioDocumentos `
                -Recurse -Force
        }
    }

    & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c 'SELECT 1;' | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible conectarse a PostgreSQL con la contrasena ingresada.'
    }

    $perfilClinicoExiste = & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c "SELECT COUNT(*) = 3 FROM information_schema.columns WHERE table_schema = 'soracare' AND table_name = 'pacientes' AND column_name IN ('antecedentes_patologicos_personales', 'farmacos_utilizados', 'alergias');"
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible verificar la migracion del perfil clinico.'
    }

    if (($perfilClinicoExiste | Out-String).Trim() -eq 't') {
        Write-Host 'La migracion del perfil clinico ya estaba aplicada.' -ForegroundColor Yellow
    } else {
        & $psql -w -h localhost -U soracare_app -d soracare_db `
            -v ON_ERROR_STOP=1 -f $migracionPerfilClinico
        if ($LASTEXITCODE -ne 0) {
            throw 'La migracion del perfil clinico no pudo completarse.'
        }
        Write-Host 'Migracion del perfil clinico aplicada.' -ForegroundColor Green
    }

    $signosVitalesExisten = & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c "SELECT COUNT(*) = 6 FROM information_schema.columns WHERE table_schema = 'soracare' AND table_name = 'atenciones_medicas' AND column_name IN ('temperatura_c', 'frecuencia_cardiaca', 'frecuencia_respiratoria', 'presion_sistolica', 'presion_diastolica', 'saturacion_oxigeno');"
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible verificar la migracion de signos vitales.'
    }

    if (($signosVitalesExisten | Out-String).Trim() -eq 't') {
        Write-Host 'La migracion de signos vitales ya estaba aplicada.' -ForegroundColor Yellow
    } else {
        & $psql -w -h localhost -U soracare_app -d soracare_db `
            -v ON_ERROR_STOP=1 -f $migracionSignosVitales
        if ($LASTEXITCODE -ne 0) {
            throw 'La migracion de signos vitales no pudo completarse.'
        }
        Write-Host 'Migracion de signos vitales aplicada.' -ForegroundColor Green
    }

    $areaAfectadaExiste = & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'soracare' AND table_name = 'atenciones_medicas' AND column_name = 'evaluacion_area_afectada');"
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible verificar la migracion del area afectada.'
    }

    if (($areaAfectadaExiste | Out-String).Trim() -eq 't') {
        Write-Host 'La migracion del area afectada ya estaba aplicada.' -ForegroundColor Yellow
    } else {
        & $psql -w -h localhost -U soracare_app -d soracare_db `
            -v ON_ERROR_STOP=1 -f $migracionAreaAfectada
        if ($LASTEXITCODE -ne 0) {
            throw 'La migracion del area afectada no pudo completarse.'
        }
        Write-Host 'Migracion del area afectada aplicada.' -ForegroundColor Green
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
    Remove-Item Env:SORACARE_DOCUMENTOS_DIR -ErrorAction SilentlyContinue
    $dbPasswordEtapa6 = $null
    $segura = $null
}
