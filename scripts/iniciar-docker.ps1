param(
    [switch]$Detener
)

$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$compose = Join-Path $proyecto 'compose.yaml'
$archivoEntorno = Join-Path $proyecto '.env.docker'

function Resolver-Docker {
    $comando = Get-Command docker -ErrorAction SilentlyContinue
    if ($comando) {
        return $comando.Source
    }

    $candidatos = @(
        "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\docker.exe",
        'C:\Program Files\Docker\Docker\resources\bin\docker.exe'
    )
    foreach ($candidato in $candidatos) {
        if (Test-Path -LiteralPath $candidato) {
            return $candidato
        }
    }
    throw 'Docker no está instalado o no puede encontrarse en el PATH.'
}

function Nuevo-Secreto {
    $bytes = New-Object byte[] 32
    $generador = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generador.GetBytes($bytes)
    } finally {
        $generador.Dispose()
    }
    return ([BitConverter]::ToString($bytes) -replace '-', '').ToLowerInvariant()
}

function Crear-EntornoLocal {
    if (Test-Path -LiteralPath $archivoEntorno) {
        return
    }

    $lineas = @(
        '# Generado localmente por scripts/iniciar-docker.ps1. No subir a Git.',
        "SORACARE_POSTGRES_ADMIN_PASSWORD=$(Nuevo-Secreto)",
        "SORACARE_DB_PASSWORD=$(Nuevo-Secreto)",
        'SORACARE_HTTP_PORT=8081'
    )
    $utf8SinBom = [Text.UTF8Encoding]::new($false)
    [IO.File]::WriteAllLines($archivoEntorno, $lineas, $utf8SinBom)
    Write-Host 'Se creó .env.docker con secretos aleatorios fuera de Git.' -ForegroundColor Green
}

function Cargar-IntegracionesUsuario {
    $variables = @(
        'SORACARE_OPENAI_API_KEY',
        'SORACARE_OPENAI_MODEL',
        'SORACARE_RESEND_API_KEY',
        'SORACARE_EMAIL_FROM',
        'SORACARE_TWILIO_ACCOUNT_SID',
        'SORACARE_TWILIO_AUTH_TOKEN',
        'SORACARE_TWILIO_FROM',
        'SORACARE_GOOGLE_CLIENT_ID',
        'SORACARE_GOOGLE_CLIENT_SECRET',
        'SORACARE_GOOGLE_CALENDAR_REDIRECT_URI',
        'SORACARE_GOOGLE_TOKEN_ENCRYPTION_KEY',
        'SORACARE_TURNSTILE_SITE_KEY',
        'SORACARE_TURNSTILE_SECRET_KEY',
        'SORACARE_SENTRY_DSN',
        'SORACARE_SENTRY_ENVIRONMENT',
        'SORACARE_FIREBASE_API_KEY',
        'SORACARE_FIREBASE_AUTH_DOMAIN',
        'SORACARE_FIREBASE_PROJECT_ID',
        'SORACARE_FIREBASE_STORAGE_BUCKET',
        'SORACARE_FIREBASE_MESSAGING_SENDER_ID',
        'SORACARE_FIREBASE_APP_ID',
        'SORACARE_FIREBASE_VAPID_KEY',
        'SORACARE_SIFEN_AMBIENTE',
        'SORACARE_SIFEN_RUC',
        'SORACARE_SIFEN_DV',
        'SORACARE_SIFEN_TIMBRADO',
        'SORACARE_SIFEN_ESTABLECIMIENTO',
        'SORACARE_SIFEN_PUNTO_EXPEDICION',
        'SORACARE_SIFEN_CERTIFICADO_PATH',
        'SORACARE_SIFEN_CERTIFICADO_PASSWORD',
        'SORACARE_SIFEN_CSC_ID',
        'SORACARE_SIFEN_CSC',
        'SORACARE_SIFEN_FECHA_INICIO_TIMBRADO',
        'SORACARE_SIFEN_TIPO_CONTRIBUYENTE',
        'SORACARE_SIFEN_TIPO_REGIMEN',
        'SORACARE_SIFEN_RAZON_SOCIAL',
        'SORACARE_SIFEN_NOMBRE_FANTASIA',
        'SORACARE_SIFEN_DIRECCION',
        'SORACARE_SIFEN_NUMERO_CASA',
        'SORACARE_SIFEN_DEPARTAMENTO',
        'SORACARE_SIFEN_DEPARTAMENTO_DESCRIPCION',
        'SORACARE_SIFEN_DISTRITO',
        'SORACARE_SIFEN_DISTRITO_DESCRIPCION',
        'SORACARE_SIFEN_CIUDAD',
        'SORACARE_SIFEN_CIUDAD_DESCRIPCION',
        'SORACARE_SIFEN_TELEFONO',
        'SORACARE_SIFEN_EMAIL',
        'SORACARE_SIFEN_ACTIVIDAD_CODIGO',
        'SORACARE_SIFEN_ACTIVIDAD_DESCRIPCION',
        'SORACARE_SIFEN_INFORMACION_EMISOR',
        'SORACARE_SIFEN_HOMOLOGACION_CONFIRMADA'
    )

    foreach ($nombre in $variables) {
        $valor = [Environment]::GetEnvironmentVariable($nombre, 'User')
        if (-not [string]::IsNullOrWhiteSpace($valor)) {
            Set-Item -Path "Env:$nombre" -Value $valor
        }
    }
}

function Mostrar-EstadoGoogleCalendar {
    $requeridas = @(
        'SORACARE_GOOGLE_CLIENT_ID',
        'SORACARE_GOOGLE_CLIENT_SECRET',
        'SORACARE_GOOGLE_CALENDAR_REDIRECT_URI',
        'SORACARE_GOOGLE_TOKEN_ENCRYPTION_KEY'
    )
    $faltantes = @(
        $requeridas | Where-Object {
            [string]::IsNullOrWhiteSpace(
                [Environment]::GetEnvironmentVariable($_, 'Process')
            )
        }
    )
    if ($faltantes.Count -eq 0) {
        Write-Host 'Google Calendar: vinculación persistente habilitada.' -ForegroundColor Green
        return
    }
    if ($faltantes.Count -lt $requeridas.Count) {
        Write-Warning (
            'Google Calendar seguirá en modo temporal. Faltan configurar: ' +
            ($faltantes -join ', ')
        )
    }
}

function Obtener-Puerto {
    $linea = Get-Content -LiteralPath $archivoEntorno |
        Where-Object { $_ -match '^SORACARE_HTTP_PORT=' } |
        Select-Object -First 1
    if (-not $linea) {
        return 8081
    }
    $valor = $linea.Split('=', 2)[1]
    $puerto = 0
    if (-not [int]::TryParse($valor, [ref]$puerto) -or $puerto -lt 1 -or $puerto -gt 65535) {
        throw 'SORACARE_HTTP_PORT no contiene un puerto válido.'
    }
    return $puerto
}

$docker = Resolver-Docker
Crear-EntornoLocal
Cargar-IntegracionesUsuario

if ($Detener) {
    & $docker compose --env-file $archivoEntorno -f $compose down
    if ($LASTEXITCODE -ne 0) {
        throw 'No fue posible detener los contenedores de SoraCare.'
    }
    Write-Host 'SoraCare Docker fue detenido; los datos permanecen guardados.' -ForegroundColor Green
    exit 0
}

& $docker info --format '{{.ServerVersion}}' *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop está instalado, pero su motor no está activo. Abrilo y esperá a que termine de iniciar.'
}

Mostrar-EstadoGoogleCalendar
Write-Host 'Construyendo y levantando SoraCare con Docker...' -ForegroundColor Cyan
& $docker compose --env-file $archivoEntorno -f $compose up --build --detach --remove-orphans
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Compose no pudo iniciar SoraCare.'
}

$puerto = Obtener-Puerto
$urlBase = "http://localhost:$puerto/SoraCare-FPUNE"
$disponible = $false
for ($intento = 1; $intento -le 90; $intento++) {
    try {
        $respuesta = Invoke-WebRequest -Uri "$urlBase/conexion-bd" -UseBasicParsing -TimeoutSec 5
        if ($respuesta.StatusCode -eq 200) {
            $disponible = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

& $docker compose --env-file $archivoEntorno -f $compose ps
if (-not $disponible) {
    throw "Los contenedores iniciaron, pero SoraCare no respondió correctamente en $urlBase."
}

Write-Host "SoraCare Docker está disponible en $urlBase/" -ForegroundColor Green
