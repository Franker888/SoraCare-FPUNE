param([switch]$Verificar)

$ErrorActionPreference = 'Stop'
$proyecto = Split-Path -Parent $PSScriptRoot
$docker = (Get-Command docker -ErrorAction SilentlyContinue).Source
if (-not $docker) { $docker = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\docker.exe" }
if (-not (Test-Path -LiteralPath $docker)) { throw 'No se encuentra Docker.' }

function Docker-Ok {
    param([string[]]$Argumentos)
    $salida = & $docker @Argumentos
    if ($LASTEXITCODE -ne 0) { throw "Docker fallo: $($Argumentos[0])." }
    return $salida
}

# No leer .env ni imprimir configuraciones que puedan contener credenciales.
$db = @(Docker-Ok @('ps', '-q', '--filter', 'label=com.docker.compose.project=soracare', '--filter', 'label=com.docker.compose.service=postgres'))
$app = @(Docker-Ok @('ps', '-q', '--filter', 'label=com.docker.compose.project=soracare', '--filter', 'label=com.docker.compose.service=aplicacion'))
if ($db.Count -ne 1 -or $app.Count -ne 1) { throw 'Debe estar ejecutandose una unica instancia de SoraCare y PostgreSQL.' }
$id = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$destino = Join-Path $proyecto "respaldos\$id"
$temporal = "/tmp/soracare-$id.dump"
$detenida = $false
$mutex = [Threading.Mutex]::new($false, 'Local\SoraCareRespaldo')
$adquirido = $false
try {
    $adquirido = $mutex.WaitOne(0)
    if (-not $adquirido) { throw 'Ya hay un respaldo en curso.' }
    New-Item -ItemType Directory -Path $destino | Out-Null
    # Evitar que otros usuarios locales hereden acceso a los datos clinicos.
    $acl = Get-Acl -LiteralPath $destino
    $acl.SetAccessRuleProtection($true, $false)
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $regla = [Security.AccessControl.FileSystemAccessRule]::new($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
    $acl.SetAccessRule($regla)
    Set-Acl -LiteralPath $destino -AclObject $acl
    Write-Host 'Pausando SoraCare para copiar base y archivos coherentes...'
    $detenida = $true
    Docker-Ok @('stop', '--time', '60', $app[0]) | Out-Null
    Docker-Ok @('exec', $db[0], 'pg_dump', '-U', 'postgres', '-d', 'soracare_db', '-Fc', '--no-owner', '--no-acl', '-f', $temporal) | Out-Null
    Docker-Ok @('cp', "${db}:$temporal", (Join-Path $destino 'base.dump')) | Out-Null
    Docker-Ok @('run', '--rm', '--network', 'none', '--volumes-from', "$($app[0]):ro", '--mount', "type=bind,source=$destino,target=/respaldo", 'postgres:18.6-bookworm', 'tar', '-czf', '/respaldo/archivos.tar.gz', '-C', '/var/lib/soracare', 'documentos-clinicos', 'fotos-perfil') | Out-Null
    $archivos = @('base.dump', 'archivos.tar.gz') | ForEach-Object {
        $ruta = Join-Path $destino $_
        @{ nombre = $_; bytes = (Get-Item -LiteralPath $ruta).Length; sha256 = (Get-FileHash -LiteralPath $ruta -Algorithm SHA256).Hash }
    }
    @{ formato = 1; fechaUtc = [DateTime]::UtcNow.ToString('o'); base = 'soracare_db'; postgres = '18.6'; archivos = @($archivos) } |
        ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $destino 'manifiesto.json') -Encoding UTF8
} finally {
    try {
        if ($detenida) {
            Docker-Ok @('start', $app[0]) | Out-Null
            # Ruta unica creada por esta ejecucion, nunca un directorio de datos.
            Docker-Ok @('exec', $db[0], 'rm', '-f', $temporal) | Out-Null
        }
    } finally {
        if ($adquirido) { $mutex.ReleaseMutex() }
        $mutex.Dispose()
    }
}
Write-Host "Respaldo completo: $destino"
$saludable = $false
for ($i = 0; $i -lt 60; $i++) {
    $estado = Docker-Ok @('inspect', '--format', '{{.State.Health.Status}}', $app[0])
    if ($estado -eq 'healthy') { $saludable = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $saludable) { throw 'El respaldo termino, pero SoraCare no recupero su estado saludable. Revisar Docker.' }
if ($Verificar) { & (Join-Path $PSScriptRoot 'verificar-respaldo-docker.ps1') -Ruta $destino }
