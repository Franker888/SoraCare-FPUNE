param([Parameter(Mandatory = $true)][string]$Ruta)

$ErrorActionPreference = 'Stop'
$Ruta = (Resolve-Path -LiteralPath $Ruta).Path
$docker = (Get-Command docker -ErrorAction SilentlyContinue).Source
if (-not $docker) { $docker = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\docker.exe" }
function Docker-Ok {
    param([string[]]$Argumentos)
    $salida = & $docker @Argumentos
    if ($LASTEXITCODE -ne 0) { throw "Docker fallo: $($Argumentos[0])." }
    return $salida
}
$manifiesto = Get-Content -LiteralPath (Join-Path $Ruta 'manifiesto.json') -Raw | ConvertFrom-Json
if ($manifiesto.formato -ne 1) { throw 'Formato de respaldo no compatible.' }
foreach ($nombre in @('base.dump', 'archivos.tar.gz')) {
    $entrada = @($manifiesto.archivos | Where-Object { $_.nombre -eq $nombre })
    $archivo = Get-Item -LiteralPath (Join-Path $Ruta $nombre)
    if ($entrada.Count -ne 1 -or $archivo.Length -ne $entrada[0].bytes -or
        (Get-FileHash -LiteralPath $archivo.FullName -Algorithm SHA256).Hash -ne $entrada[0].sha256) {
        throw "Respaldo incompleto o alterado: $nombre."
    }
}
# Instancia temporal sin red, sin puertos y sin volumen persistente.
# Solo se restauran copias creadas localmente y de confianza.
$nombre = 'soracare-restauracion-' + [guid]::NewGuid().ToString('N')
$creado = $false
try {
    Docker-Ok @('create', '--name', $nombre, '--label', "soracare.prueba=$nombre", '--network', 'none', '--tmpfs', '/var/lib/postgresql', '--tmpfs', '/restaurado', '--mount', "type=bind,source=$Ruta,target=/respaldo,readonly", '-e', 'POSTGRES_HOST_AUTH_METHOD=trust', 'postgres:18.6-bookworm') | Out-Null
    $creado = $true
    Docker-Ok @('start', $nombre) | Out-Null
    $listo = $false
    for ($i = 0; $i -lt 60; $i++) {
        & $docker exec $nombre pg_isready -U postgres *> $null
        if ($LASTEXITCODE -eq 0) { $listo = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $listo) { throw 'PostgreSQL de prueba no inicio.' }
    Docker-Ok @('exec', $nombre, 'createdb', '-U', 'postgres', 'restauracion') | Out-Null
    Docker-Ok @('exec', $nombre, 'pg_restore', '-U', 'postgres', '-d', 'restauracion', '--exit-on-error', '--no-owner', '--no-acl', '/respaldo/base.dump') | Out-Null
    Docker-Ok @('exec', $nombre, 'tar', '-xzf', '/respaldo/archivos.tar.gz', '-C', '/restaurado', '--no-same-owner') | Out-Null
    Docker-Ok @('exec', $nombre, 'tar', '-dzf', '/respaldo/archivos.tar.gz', '-C', '/restaurado') | Out-Null
    $migracion = Docker-Ok @('exec', $nombre, 'psql', '-U', 'postgres', '-d', 'restauracion', '-At', '-v', 'ON_ERROR_STOP=1', '-c', 'SELECT version FROM soracare.flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1;')
    if (-not $migracion) { throw 'No se encontro el historial Flyway restaurado.' }
} finally {
    if ($creado) {
        $etiquetas = (Docker-Ok @('inspect', '--format', '{{json .Config.Labels}}', $nombre)) | ConvertFrom-Json
        if ($etiquetas.'soracare.prueba' -ne $nombre) { throw 'No se elimina un contenedor cuya etiqueta no coincide.' }
        Docker-Ok @('rm', '-f', $nombre) | Out-Null
    }
}
@{ fechaUtc = [DateTime]::UtcNow.ToString('o'); resultado = 'restauracion aislada correcta'; migracion = "$migracion" } |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Ruta 'verificacion.json') -Encoding UTF8
Write-Host "Restauracion aislada correcta. Flyway: $migracion. Archivos extraidos y comparados. Contenedor temporal eliminado."
