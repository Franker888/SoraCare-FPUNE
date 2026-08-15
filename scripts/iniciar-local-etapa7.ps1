$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$tomcat = 'C:\Users\adria\Desktop\Hospital\Servidor\apache-tomcat-10.1.57'
$javaHomeEtapa7 = 'C:\Users\adria\.jdks\ms-21.0.12'
$maven = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
$repositorioMaven = 'C:\Users\adria\.m2\repository'
$directorioDocumentos = 'C:\Users\adria\Desktop\Hospital\Datos\SoraCare\documentos-clinicos'
$war = Join-Path $proyecto 'target\SoraCare-FPUNE.war'

$migraciones = @(
    @{
        Nombre = 'catalogo de medicamentos y recetas'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V012__crear_recetas_y_medicamentos.sql'
        Verificacion = "SELECT to_regclass('soracare.medicamentos') IS NOT NULL AND to_regclass('soracare.recetas_medicas') IS NOT NULL AND to_regclass('soracare.recetas_detalles') IS NOT NULL;"
    },
    @{
        Nombre = 'infraestructura de notificaciones'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V013__crear_notificaciones.sql'
        Verificacion = "SELECT to_regclass('soracare.notificaciones') IS NOT NULL AND to_regclass('soracare.plantillas_notificacion') IS NOT NULL;"
    },
    @{
        Nombre = 'auditoria de anulacion de recetas'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V024__auditar_anulacion_recetas.sql'
        Verificacion = "SELECT COUNT(*) = 3 FROM information_schema.columns WHERE table_schema = 'soracare' AND table_name = 'recetas_medicas' AND column_name IN ('fecha_anulacion', 'motivo_anulacion', 'id_usuario_anulacion');"
    },
    @{
        Nombre = 'notificaciones internas de la etapa 7'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V025__completar_notificaciones_internas.sql'
        Verificacion = "SELECT (SELECT COUNT(*) = 2 FROM information_schema.columns WHERE table_schema = 'soracare' AND table_name = 'notificaciones' AND column_name IN ('id_receta', 'fecha_lectura')) AND (SELECT COUNT(*) = 5 FROM soracare.plantillas_notificacion WHERE codigo IN ('RECETA_EMITIDA_SISTEMA', 'CITA_SOLICITADA_SISTEMA', 'CITA_REPROGRAMADA_SISTEMA', 'CITA_CANCELADA_SISTEMA', 'PAGO_CONFIRMADO_SISTEMA'));"
    },
    @{
        Nombre = 'estudios clinicos solicitados en la atencion'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V026__agregar_estudios_clinicos_solicitados.sql'
        Verificacion = "SELECT COUNT(*) = 1 FROM information_schema.columns WHERE table_schema = 'soracare' AND table_name = 'atenciones_medicas' AND column_name = 'estudios_clinicos_solicitados';"
    }
)

foreach ($ruta in @($psql, $tomcat, $javaHomeEtapa7, $maven)) {
    if (-not (Test-Path -LiteralPath $ruta)) {
        throw "No se encontro el recurso requerido: $ruta"
    }
}
foreach ($migracion in $migraciones) {
    if (-not (Test-Path -LiteralPath $migracion.Archivo)) {
        throw "No se encontro la migracion requerida: $($migracion.Archivo)"
    }
}

$segura = Read-Host 'Contrasena de PostgreSQL para soracare_app' -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)

try {
    $dbPasswordEtapa7 = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    $env:PGPASSWORD = $dbPasswordEtapa7
    $env:SORACARE_DB_PASSWORD = $dbPasswordEtapa7
    $env:SORACARE_DOCUMENTOS_DIR = $directorioDocumentos
    $env:JAVA_HOME = $javaHomeEtapa7
    $env:Path = "$javaHomeEtapa7\bin;$env:Path"
    $env:CATALINA_HOME = $tomcat

    New-Item -ItemType Directory -Path $directorioDocumentos -Force | Out-Null

    & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c 'SELECT 1;' | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible conectarse a PostgreSQL con la contrasena ingresada.'
    }

    foreach ($migracion in $migraciones) {
        $existe = & $psql -w -h localhost -U soracare_app -d soracare_db `
            -tA -v ON_ERROR_STOP=1 -c $migracion.Verificacion
        if ($LASTEXITCODE -ne 0) {
            throw "No fue posible verificar la migracion: $($migracion.Nombre)."
        }

        if (($existe | Out-String).Trim() -eq 't') {
            Write-Host "La migracion de $($migracion.Nombre) ya estaba aplicada." `
                -ForegroundColor Yellow
        } else {
            & $psql -w -h localhost -U soracare_app -d soracare_db `
                -v ON_ERROR_STOP=1 -f $migracion.Archivo
            if ($LASTEXITCODE -ne 0) {
                throw "No se pudo aplicar la migracion: $($migracion.Nombre)."
            }
            Write-Host "Migracion aplicada: $($migracion.Nombre)." -ForegroundColor Green
        }
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
        Write-Host 'Tomcat ya estaba detenido; se continuara con el despliegue.' `
            -ForegroundColor Yellow
    }

    Write-Host 'Compilando SoraCare desde el codigo actual...' -ForegroundColor Cyan
    & $maven "-Dmaven.repo.local=$repositorioMaven" package
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'La primera compilacion no termino; se realizara un segundo intento.' `
            -ForegroundColor Yellow
        & $maven "-Dmaven.repo.local=$repositorioMaven" package
        if ($LASTEXITCODE -ne 0) {
            throw 'No fue posible compilar el proyecto para el despliegue.'
        }
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
    Start-Process -FilePath $startup -WorkingDirectory (Join-Path $tomcat 'bin') `
        -WindowStyle Hidden
    Start-Sleep -Seconds 3

    Write-Host 'Etapa 7 desplegada. Abri http://localhost:8080/SoraCare-FPUNE/' `
        -ForegroundColor Green
} finally {
    if ($puntero -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    }
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_DOCUMENTOS_DIR -ErrorAction SilentlyContinue
    $dbPasswordEtapa7 = $null
    $segura = $null
}
