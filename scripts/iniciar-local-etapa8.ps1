$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$tomcat = 'C:\Users\adria\Desktop\Hospital\Servidor\apache-tomcat-10.1.57'
$javaHomeEtapa8 = 'C:\Users\adria\.jdks\ms-21.0.12'
$maven = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
$repositorioMaven = 'C:\Users\adria\.m2\repository'
$directorioDocumentos = 'C:\Users\adria\Desktop\Hospital\Datos\SoraCare\documentos-clinicos'
$directorioPerfiles = 'C:\Users\adria\Desktop\Hospital\Datos\SoraCare\fotos-perfil'
$war = Join-Path $proyecto 'target\SoraCare-FPUNE.war'

$migraciones = @(
    @{
        Nombre = 'bitacora de auditoria'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V005__crear_auditoria.sql'
        Verificacion = "SELECT to_regclass('soracare.auditoria_eventos') IS NOT NULL;"
    },
    @{
        Nombre = 'infraestructura de reportes'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V015__crear_reportes.sql'
        Verificacion = "SELECT to_regclass('soracare.tipos_reportes') IS NOT NULL AND to_regclass('soracare.reportes_generados') IS NOT NULL AND to_regclass('soracare.accesos_reportes') IS NOT NULL;"
    },
    @{
        Nombre = 'configuracion general del sistema'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V016__crear_configuracion_sistema.sql'
        Verificacion = "SELECT to_regclass('soracare.configuraciones_sistema') IS NOT NULL AND to_regclass('soracare.historial_configuraciones') IS NOT NULL;"
    },
    @{
        Nombre = 'analitica anonima de disponibilidad'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V027__registrar_busquedas_disponibilidad.sql'
        Verificacion = "SELECT to_regclass('soracare.busquedas_disponibilidad') IS NOT NULL;"
    },
    @{
        Nombre = 'perfiles visuales de medicos y pacientes'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V028__crear_perfiles_visuales.sql'
        Verificacion = "SELECT to_regclass('soracare.perfiles_visuales') IS NOT NULL;"
    },
    @{
        Nombre = 'correo real de comprobantes con Resend'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V029__habilitar_correo_real_resend.sql'
        Verificacion = "SELECT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_comprobantes_estado_email' AND pg_get_constraintdef(oid) LIKE '%NO_CONFIGURADO%');"
    },
    @{
        Nombre = 'correo de indicaciones medicas con Resend'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V030__enviar_indicaciones_medicas_resend.sql'
        Verificacion = "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'soracare' AND table_name = 'atenciones_medicas' AND column_name = 'estado_envio_indicaciones_email');"
    },
    @{
        Nombre = 'identidades verificadas de Google'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V031__crear_identidades_google.sql'
        Verificacion = "SELECT to_regclass('soracare.identidades_externas') IS NOT NULL;"
    },
    @{
        Nombre = 'suscripciones web de Firebase Cloud Messaging'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V032__crear_suscripciones_push.sql'
        Verificacion = "SELECT to_regclass('soracare.suscripciones_push') IS NOT NULL;"
    },
    @{
        Nombre = 'facturacion comercial interna'
        Archivo = Join-Path $proyecto 'src\main\resources\db\migration\V033__crear_facturacion_interna.sql'
        Verificacion = "SELECT to_regclass('soracare.facturas_internas') IS NOT NULL AND to_regclass('soracare.facturas_internas_detalles') IS NOT NULL;"
    }
)

foreach ($ruta in @($psql, $tomcat, $javaHomeEtapa8, $maven)) {
    if (-not (Test-Path -LiteralPath $ruta)) {
        throw "No se encontro el recurso requerido: $ruta"
    }
}

$segura = Read-Host 'Contrasena de PostgreSQL para soracare_app' -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)

try {
    $dbPasswordEtapa8 = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    $env:PGPASSWORD = $dbPasswordEtapa8
    $env:SORACARE_DB_PASSWORD = $dbPasswordEtapa8
    $env:SORACARE_DOCUMENTOS_DIR = $directorioDocumentos
    $env:SORACARE_PERFILES_DIR = $directorioPerfiles
    $resendApiKeyUsuario = [Environment]::GetEnvironmentVariable('SORACARE_RESEND_API_KEY', 'User')
    $remitenteCorreoUsuario = [Environment]::GetEnvironmentVariable('SORACARE_EMAIL_FROM', 'User')
    if (-not [string]::IsNullOrWhiteSpace($resendApiKeyUsuario)) {
        $env:SORACARE_RESEND_API_KEY = $resendApiKeyUsuario
        if (-not [string]::IsNullOrWhiteSpace($remitenteCorreoUsuario)) {
            $env:SORACARE_EMAIL_FROM = $remitenteCorreoUsuario
        }
        Write-Host 'Integracion Resend disponible para esta ejecucion.' -ForegroundColor Green
    } else {
        Write-Host 'Resend aun no esta configurado; el resto de SoraCare funcionara normalmente.' -ForegroundColor Yellow
    }
    $twilioAccountSidUsuario = [Environment]::GetEnvironmentVariable('SORACARE_TWILIO_ACCOUNT_SID', 'User')
    $twilioAuthTokenUsuario = [Environment]::GetEnvironmentVariable('SORACARE_TWILIO_AUTH_TOKEN', 'User')
    $twilioFromUsuario = [Environment]::GetEnvironmentVariable('SORACARE_TWILIO_FROM', 'User')
    if (-not [string]::IsNullOrWhiteSpace($twilioAccountSidUsuario) -and
        -not [string]::IsNullOrWhiteSpace($twilioAuthTokenUsuario) -and
        -not [string]::IsNullOrWhiteSpace($twilioFromUsuario)) {
        $env:SORACARE_TWILIO_ACCOUNT_SID = $twilioAccountSidUsuario
        $env:SORACARE_TWILIO_AUTH_TOKEN = $twilioAuthTokenUsuario
        $env:SORACARE_TWILIO_FROM = $twilioFromUsuario
        Write-Host 'Integracion Twilio SMS disponible para esta ejecucion.' -ForegroundColor Green
    } else {
        Write-Host 'Twilio SMS aun no esta configurado; el resto de SoraCare funcionara normalmente.' -ForegroundColor Yellow
    }
    $googleClientIdUsuario = [Environment]::GetEnvironmentVariable('SORACARE_GOOGLE_CLIENT_ID', 'User')
    if (-not [string]::IsNullOrWhiteSpace($googleClientIdUsuario)) {
        $env:SORACARE_GOOGLE_CLIENT_ID = $googleClientIdUsuario
        Write-Host 'Acceso con Google y sincronizacion opcional de Calendar disponibles.' -ForegroundColor Green
    } else {
        Write-Host 'Google aun no esta configurado; el acceso tradicional funcionara normalmente.' -ForegroundColor Yellow
    }
    $turnstileSiteKeyUsuario = [Environment]::GetEnvironmentVariable('SORACARE_TURNSTILE_SITE_KEY', 'User')
    $turnstileSecretKeyUsuario = [Environment]::GetEnvironmentVariable('SORACARE_TURNSTILE_SECRET_KEY', 'User')
    if (-not [string]::IsNullOrWhiteSpace($turnstileSiteKeyUsuario)) {
        $env:SORACARE_TURNSTILE_SITE_KEY = $turnstileSiteKeyUsuario
    }
    if (-not [string]::IsNullOrWhiteSpace($turnstileSecretKeyUsuario)) {
        $env:SORACARE_TURNSTILE_SECRET_KEY = $turnstileSecretKeyUsuario
    }
    if (-not [string]::IsNullOrWhiteSpace($turnstileSiteKeyUsuario) -and
        -not [string]::IsNullOrWhiteSpace($turnstileSecretKeyUsuario)) {
        Write-Host 'Cloudflare Turnstile protege el login y el registro tradicional.' -ForegroundColor Green
    } elseif (-not [string]::IsNullOrWhiteSpace($turnstileSiteKeyUsuario) -or
              -not [string]::IsNullOrWhiteSpace($turnstileSecretKeyUsuario)) {
        Write-Host 'Turnstile tiene una configuracion incompleta: login y registro se bloquearan hasta corregirla.' -ForegroundColor Red
    } else {
        Write-Host 'Turnstile aun no esta configurado; los formularios seguiran disponibles en modo local.' -ForegroundColor Yellow
    }
    $sentryDsnUsuario = [Environment]::GetEnvironmentVariable('SORACARE_SENTRY_DSN', 'User')
    $sentryAmbienteUsuario = [Environment]::GetEnvironmentVariable('SORACARE_SENTRY_ENVIRONMENT', 'User')
    if (-not [string]::IsNullOrWhiteSpace($sentryDsnUsuario)) {
        $env:SORACARE_SENTRY_DSN = $sentryDsnUsuario
        if (-not [string]::IsNullOrWhiteSpace($sentryAmbienteUsuario)) {
            $env:SORACARE_SENTRY_ENVIRONMENT = $sentryAmbienteUsuario
        }
        Write-Host 'Sentry monitoreara errores tecnicos anonimizados durante esta ejecucion.' -ForegroundColor Green
    } else {
        Write-Host 'Sentry aun no esta configurado; SoraCare funcionara normalmente sin monitoreo remoto.' -ForegroundColor Yellow
    }
    $firebaseVariables = @(
        'SORACARE_FIREBASE_API_KEY',
        'SORACARE_FIREBASE_AUTH_DOMAIN',
        'SORACARE_FIREBASE_PROJECT_ID',
        'SORACARE_FIREBASE_STORAGE_BUCKET',
        'SORACARE_FIREBASE_MESSAGING_SENDER_ID',
        'SORACARE_FIREBASE_APP_ID',
        'SORACARE_FIREBASE_VAPID_KEY'
    )
    $firebaseCompletas = $true
    foreach ($firebaseVariable in $firebaseVariables) {
        $firebaseValor = [Environment]::GetEnvironmentVariable($firebaseVariable, 'User')
        if ([string]::IsNullOrWhiteSpace($firebaseValor)) {
            $firebaseCompletas = $false
        } else {
            Set-Item -Path "Env:$firebaseVariable" -Value $firebaseValor
        }
    }
    if ($firebaseCompletas) {
        Write-Host 'Firebase Cloud Messaging disponible para suscribir navegadores y telefonos.' -ForegroundColor Green
    } else {
        Write-Host 'Firebase Cloud Messaging aun no esta completo; los avisos internos seguiran funcionando.' -ForegroundColor Yellow
    }
    $openAiApiKeyUsuario = [Environment]::GetEnvironmentVariable('SORACARE_OPENAI_API_KEY', 'User')
    $openAiModeloUsuario = [Environment]::GetEnvironmentVariable('SORACARE_OPENAI_MODEL', 'User')
    if (-not [string]::IsNullOrWhiteSpace($openAiApiKeyUsuario)) {
        $env:SORACARE_OPENAI_API_KEY = $openAiApiKeyUsuario
        if (-not [string]::IsNullOrWhiteSpace($openAiModeloUsuario)) {
            $env:SORACARE_OPENAI_MODEL = $openAiModeloUsuario
        }
        Write-Host 'Sora con OpenAI disponible para esta ejecucion.' -ForegroundColor Green
    } else {
        Write-Host 'OpenAI aun no esta configurado; Sora usara su orientacion local segura.' -ForegroundColor Yellow
    }
    $env:JAVA_HOME = $javaHomeEtapa8
    $env:Path = "$javaHomeEtapa8\bin;$env:Path"
    $env:CATALINA_HOME = $tomcat

    New-Item -ItemType Directory -Path $directorioDocumentos -Force | Out-Null
    New-Item -ItemType Directory -Path $directorioPerfiles -Force | Out-Null

    & $psql -w -h localhost -U soracare_app -d soracare_db `
        -tA -v ON_ERROR_STOP=1 -c 'SELECT 1;' | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible conectarse a PostgreSQL con la contrasena ingresada.'
    }

    foreach ($migracion in $migraciones) {
        $aplicada = & $psql -w -h localhost -U soracare_app -d soracare_db `
            -tA -v ON_ERROR_STOP=1 -c $migracion.Verificacion
        if ($LASTEXITCODE -ne 0) {
            throw "No fue posible verificar la migracion: $($migracion.Nombre)."
        }
        if (($aplicada | Out-String).Trim() -eq 't') {
            Write-Host "La migracion de $($migracion.Nombre) ya estaba aplicada." -ForegroundColor Yellow
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
        Write-Host 'Tomcat ya estaba detenido; se continuara con el despliegue.' -ForegroundColor Yellow
    }

    Write-Host 'Compilando la administracion integral de la etapa 8...' -ForegroundColor Cyan
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
    Copy-Item -LiteralPath $war -Destination (Join-Path $webapps 'SoraCare-FPUNE.war') -Force
    Start-Process -FilePath $startup -WorkingDirectory (Join-Path $tomcat 'bin') -WindowStyle Hidden

    $urlBase = 'http://localhost:8080/SoraCare-FPUNE'
    $aplicacionDisponible = $false
    for ($intento = 1; $intento -le 30; $intento++) {
        try {
            $respuesta = Invoke-WebRequest -Uri "$urlBase/" -UseBasicParsing -TimeoutSec 5
            if ($respuesta.StatusCode -eq 200) {
                $aplicacionDisponible = $true
                break
            }
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    if (-not $aplicacionDisponible) {
        throw 'Tomcat inicio, pero SoraCare no respondio dentro del tiempo esperado.'
    }

    Write-Host 'Preparando las pantallas de uso frecuente...' -ForegroundColor Cyan
    foreach ($rutaFrecuente in @('/login', '/medicos', '/especialidades', '/disponibilidad')) {
        try {
            Invoke-WebRequest -Uri "$urlBase$rutaFrecuente" -UseBasicParsing -TimeoutSec 30 | Out-Null
        } catch {
            Write-Host "No se pudo preparar anticipadamente $rutaFrecuente; se compilara al abrirla." -ForegroundColor Yellow
        }
    }

    Write-Host 'Etapa 8 desplegada. Inicia sesion en http://localhost:8080/SoraCare-FPUNE/login' -ForegroundColor Green
} finally {
    if ($puntero -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    }
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_DOCUMENTOS_DIR -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_PERFILES_DIR -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_RESEND_API_KEY -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_EMAIL_FROM -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_TWILIO_ACCOUNT_SID -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_TWILIO_AUTH_TOKEN -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_TWILIO_FROM -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_GOOGLE_CLIENT_ID -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_TURNSTILE_SITE_KEY -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_TURNSTILE_SECRET_KEY -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_SENTRY_DSN -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_SENTRY_ENVIRONMENT -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_FIREBASE_API_KEY -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_FIREBASE_AUTH_DOMAIN -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_FIREBASE_PROJECT_ID -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_FIREBASE_STORAGE_BUCKET -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_FIREBASE_MESSAGING_SENDER_ID -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_FIREBASE_APP_ID -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_FIREBASE_VAPID_KEY -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_OPENAI_API_KEY -ErrorAction SilentlyContinue
    Remove-Item Env:SORACARE_OPENAI_MODEL -ErrorAction SilentlyContinue
    $dbPasswordEtapa8 = $null
    $segura = $null
}
