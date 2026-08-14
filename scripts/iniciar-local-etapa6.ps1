$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$migracionPerfilClinico = Join-Path $proyecto 'src\main\resources\db\migration\V019__agregar_perfil_clinico_paciente.sql'
$migracionSignosVitales = Join-Path $proyecto 'src\main\resources\db\migration\V020__agregar_signos_vitales_atencion.sql'
$migracionAreaAfectada = Join-Path $proyecto 'src\main\resources\db\migration\V021__agregar_evaluacion_area_afectada.sql'
$migracionReprogramaciones = Join-Path $proyecto 'src\main\resources\db\migration\V022__registrar_reprogramaciones_citas.sql'
$migracionPresentacionMedico = Join-Path $proyecto 'src\main\resources\db\migration\V023__agregar_presentacion_visual_medico.sql'
$war = Join-Path $proyecto 'target\SoraCare-FPUNE.war'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$tomcat = 'C:\Users\adria\Desktop\Hospital\Servidor\apache-tomcat-10.1.57'
$javaHomeEtapa6 = 'C:\Users\adria\.jdks\ms-21.0.12'
$maven = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
$repositorioMaven = 'C:\Users\adria\.m2\repository'
$directorioDocumentos = 'C:\Users\adria\Desktop\Hospital\Datos\SoraCare\documentos-clinicos'
$directorioDocumentosAnterior = Join-Path ([Environment]::GetFolderPath('UserProfile')) `
    '.soracare\documentos-clinicos'

foreach ($ruta in @(
    $migracionPerfilClinico,
    $migracionSignosVitales,
    $migracionAreaAfectada,
    $migracionReprogramaciones,
    $migracionPresentacionMedico,
    $psql,
    $tomcat,
    $javaHomeEtapa6,
    $maven
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

    $reprogramacionesExisten = & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c "SELECT to_regclass('soracare.reprogramaciones_citas') IS NOT NULL;"
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible verificar la migracion de reprogramaciones.'
    }

    if (($reprogramacionesExisten | Out-String).Trim() -eq 't') {
        Write-Host 'La migracion de reprogramaciones ya estaba aplicada.' -ForegroundColor Yellow
    } else {
        & $psql -w -h localhost -U soracare_app -d soracare_db `
            -v ON_ERROR_STOP=1 -f $migracionReprogramaciones
        if ($LASTEXITCODE -ne 0) {
            throw 'La migracion de reprogramaciones no pudo completarse.'
        }
        Write-Host 'Migracion de reprogramaciones aplicada.' -ForegroundColor Green
    }

    $presentacionMedicoExiste = & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c "SELECT COUNT(*) = 2 FROM information_schema.columns WHERE table_schema = 'soracare' AND table_name = 'medicos' AND column_name IN ('tratamiento', 'foto_perfil');"
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible verificar la migracion de presentacion del medico.'
    }

    if (($presentacionMedicoExiste | Out-String).Trim() -eq 't') {
        Write-Host 'La migracion de presentacion del medico ya estaba aplicada.' -ForegroundColor Yellow
    } else {
        & $psql -w -h localhost -U soracare_app -d soracare_db `
            -v ON_ERROR_STOP=1 -f $migracionPresentacionMedico
        if ($LASTEXITCODE -ne 0) {
            throw 'La migracion de presentacion del medico no pudo completarse.'
        }
        Write-Host 'Migracion de presentacion del medico aplicada.' -ForegroundColor Green
    }

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
        Start-Sleep -Seconds 3
    } else {
        Write-Host 'Tomcat ya estaba detenido; se continuara con el inicio.' `
            -ForegroundColor Yellow
    }

    $env:JAVA_HOME = $javaHomeEtapa6
    $env:Path = "$javaHomeEtapa6\bin;$env:Path"

    Write-Host 'Compilando SoraCare desde el codigo actual...' -ForegroundColor Cyan
    & $maven "-Dmaven.repo.local=$repositorioMaven" clean package
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible compilar el proyecto para el despliegue.'
    }

    if (-not (Test-Path -LiteralPath $war)) {
        throw "La compilacion no genero el archivo esperado: $war"
    }

    $webapps = Join-Path $tomcat 'webapps'
    $destinoAplicacion = Join-Path $webapps 'SoraCare-FPUNE'
    $webappsResuelto = [System.IO.Path]::GetFullPath($webapps)
    $despliegueResuelto = [System.IO.Path]::GetFullPath($destinoAplicacion)

    if ([System.IO.Path]::GetDirectoryName($despliegueResuelto) -ne $webappsResuelto) {
        throw 'La ruta del despliegue no pertenece al directorio webapps esperado.'
    }

    if (Test-Path -LiteralPath $despliegueResuelto) {
        Remove-Item -LiteralPath $despliegueResuelto -Recurse -Force
    }

    $destinoWar = Join-Path $webapps 'SoraCare-FPUNE.war'
    Copy-Item -LiteralPath $war -Destination $destinoWar -Force

    $env:CATALINA_HOME = $tomcat
    Start-Process -FilePath $startup -WorkingDirectory (Join-Path $tomcat 'bin') `
        -WindowStyle Hidden
    Start-Sleep -Seconds 3

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
