param(
    [string]$ImagenPostgreSql = 'postgres:18.6-bookworm'
)

$ErrorActionPreference = 'Stop'
$proyecto = Split-Path -Parent $PSScriptRoot
$marcaTiempo = Get-Date -Format 'yyyyMMdd-HHmmss'
$destino = Join-Path $proyecto "respaldos\railway\$marcaTiempo"
$archivoDump = Join-Path $destino 'soracare-railway.dump'

function Resolver-Railway {
    $comando = Get-Command railway -ErrorAction SilentlyContinue
    if ($comando) {
        return $comando.Source
    }
    $candidato = Get-ChildItem -LiteralPath $env:TEMP `
        -Filter 'railway.exe' -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object FullName -Like '*soracare-railway-cli-*' |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    if ($candidato) {
        return $candidato
    }
    throw 'No se encontró Railway CLI.'
}

function Obtener-PuertoLibre {
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try {
        return ([Net.IPEndPoint]$listener.LocalEndpoint).Port
    } finally {
        $listener.Stop()
    }
}

function Esperar-Puerto([int]$puerto, [Diagnostics.Process]$procesoTunel) {
    $limite = [DateTime]::UtcNow.AddSeconds(30)
    while ([DateTime]::UtcNow -lt $limite) {
        if ($procesoTunel.HasExited) {
            throw 'Railway cerró el túnel antes de iniciar el respaldo.'
        }
        $cliente = [Net.Sockets.TcpClient]::new()
        try {
            $tarea = $cliente.ConnectAsync('127.0.0.1', $puerto)
            if ($tarea.Wait(500) -and $cliente.Connected) {
                return
            }
        } catch {
            # El túnel todavía está iniciando.
        } finally {
            $cliente.Dispose()
        }
        Start-Sleep -Milliseconds 500
    }
    throw 'El túnel privado no estuvo disponible dentro de 30 segundos.'
}

$railway = Resolver-Railway
$estadoDocker = docker version --format '{{.Server.Version}}' 2>$null
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($estadoDocker)) {
    throw 'Docker Desktop no está disponible.'
}

$variables = & $railway variables --service Postgres `
    --environment production --json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) {
    throw 'No fue posible leer la conexión privada de PostgreSQL.'
}
foreach ($nombre in @('PGUSER', 'PGPASSWORD', 'PGDATABASE')) {
    if ([string]::IsNullOrWhiteSpace($variables.$nombre)) {
        throw "Railway no proporcionó la variable $nombre."
    }
}

New-Item -ItemType Directory -Path $destino -Force *> $null
$puerto = Obtener-PuertoLibre
$salidaTunel = Join-Path $env:TEMP "soracare-backup-tunel-$puerto.out.log"
$errorTunel = Join-Path $env:TEMP "soracare-backup-tunel-$puerto.err.log"
$tunel = $null
$anteriorPgPassword = $env:PGPASSWORD
try {
    $tunel = Start-Process -FilePath $railway -ArgumentList @(
        'connect', 'Postgres', '--environment', 'production',
        '--tunnel-only', '--port', "$puerto"
    ) -WindowStyle Hidden -RedirectStandardOutput $salidaTunel `
        -RedirectStandardError $errorTunel -PassThru
    Esperar-Puerto $puerto $tunel

    $env:PGPASSWORD = $variables.PGPASSWORD
    & docker run --rm --pull never `
        --env PGPASSWORD `
        --volume "${destino}:/backup" `
        $ImagenPostgreSql `
        pg_dump --host host.docker.internal --port $puerto `
        --username $variables.PGUSER --dbname $variables.PGDATABASE `
        --format custom --no-owner --file /backup/soracare-railway.dump
    if ($LASTEXITCODE -ne 0) {
        throw 'pg_dump no pudo crear el respaldo de Railway.'
    }

    & docker run --rm --pull never `
        --volume "${destino}:/backup:ro" `
        $ImagenPostgreSql `
        pg_restore --list /backup/soracare-railway.dump *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'El archivo creado no superó la verificación de pg_restore.'
    }

    $archivo = Get-Item -LiteralPath $archivoDump
    if ($archivo.Length -lt 1024) {
        throw 'El respaldo creado es demasiado pequeño para ser válido.'
    }
    $hash = Get-FileHash -LiteralPath $archivoDump -Algorithm SHA256
    @(
        'SoraCare Railway - respaldo PostgreSQL'
        "Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
        "Archivo: $($archivo.Name)"
        "Bytes: $($archivo.Length)"
        "SHA256: $($hash.Hash)"
        'Verificacion: pg_restore --list correcta'
    ) | Set-Content -LiteralPath (Join-Path $destino 'manifest.txt') `
        -Encoding UTF8
} finally {
    if ($tunel -and -not $tunel.HasExited) {
        Stop-Process -Id $tunel.Id -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $salidaTunel, $errorTunel `
        -Force -ErrorAction SilentlyContinue
    $env:PGPASSWORD = $anteriorPgPassword
    $variables = $null
}

Write-Host ("Respaldo Railway verificado: {0}" -f $destino) `
    -ForegroundColor Green
