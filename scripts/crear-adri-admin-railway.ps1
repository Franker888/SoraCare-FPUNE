$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$sql = Join-Path $PSScriptRoot 'sql\crear-adri-admin-railway.sql'
$fuenteHash = Join-Path $PSScriptRoot 'support\GenerarHashBCrypt.java'
$fuenteJdbc = Join-Path $PSScriptRoot 'support\EjecutarSqlJdbc.java'
$clasesTemporales = Join-Path $proyecto 'target\adri-admin-railway-support'
$archivoCredencial = Join-Path $proyecto '.env.adri-admin-railway.clixml'
$jarBCrypt = Join-Path $env:USERPROFILE `
    '.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar'
$jarPostgreSql = Get-ChildItem -LiteralPath (
    Join-Path $env:USERPROFILE '.m2\repository\org\postgresql\postgresql'
) -Filter 'postgresql-*.jar' -Recurse -File |
    Where-Object Name -NotMatch '-(sources|javadoc)\.jar$' |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1 -ExpandProperty FullName

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
    throw 'No se encontro Railway CLI. Inicia sesion nuevamente en Railway.'
}

function Resolver-Java([string]$nombre) {
    $comando = Get-Command $nombre -ErrorAction SilentlyContinue
    if ($comando) {
        return $comando.Source
    }

    $candidatos = @()
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidatos += Join-Path $env:JAVA_HOME "bin\$nombre.exe"
    }
    $candidatos += Join-Path $env:USERPROFILE ".jdks\ms-21.0.12\bin\$nombre.exe"
    foreach ($candidato in $candidatos) {
        if (Test-Path -LiteralPath $candidato) {
            return $candidato
        }
    }
    throw "No se encontro $nombre. Instala o configura Java 21."
}

function Obtener-PuertoLibre {
    $listener = [Net.Sockets.TcpListener]::new(
        [Net.IPAddress]::Loopback,
        0
    )
    try {
        $listener.Start()
        return ([Net.IPEndPoint]$listener.LocalEndpoint).Port
    } finally {
        $listener.Stop()
    }
}

function Esperar-Puerto(
    [int]$puerto,
    [Diagnostics.Process]$proceso
) {
    $limite = [DateTime]::UtcNow.AddSeconds(30)
    while ([DateTime]::UtcNow -lt $limite) {
        if ($proceso.HasExited) {
            throw 'Railway cerro el tunel antes de conectar con PostgreSQL.'
        }
        $cliente = [Net.Sockets.TcpClient]::new()
        try {
            $tarea = $cliente.ConnectAsync('127.0.0.1', $puerto)
            if ($tarea.Wait(500) -and $cliente.Connected) {
                return
            }
        } catch {
            # El tunel todavia esta iniciando.
        } finally {
            $cliente.Dispose()
        }
        Start-Sleep -Milliseconds 500
    }
    throw 'El tunel de Railway no estuvo listo dentro de 30 segundos.'
}

function Nueva-ContrasenaTemporal {
    $mayusculas = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
    $minusculas = 'abcdefghijkmnopqrstuvwxyz'
    $numeros = '23456789'
    $especiales = '!@#$%&*+-_?'
    $todos = $mayusculas + $minusculas + $numeros + $especiales
    $caracteres = [Collections.Generic.List[char]]::new()
    $aleatorio = [Security.Cryptography.RandomNumberGenerator]::Create()
    $byte = New-Object byte[] 1
    try {
        foreach ($grupo in @($mayusculas, $minusculas, $numeros, $especiales)) {
            $limite = 256 - (256 % $grupo.Length)
            do {
                $aleatorio.GetBytes($byte)
            } while ($byte[0] -ge $limite)
            $caracteres.Add($grupo[$byte[0] % $grupo.Length])
        }
        while ($caracteres.Count -lt 20) {
            $limite = 256 - (256 % $todos.Length)
            do {
                $aleatorio.GetBytes($byte)
            } while ($byte[0] -ge $limite)
            $caracteres.Add($todos[$byte[0] % $todos.Length])
        }
        for ($i = $caracteres.Count - 1; $i -gt 0; $i--) {
            $limite = 256 - (256 % ($i + 1))
            do {
                $aleatorio.GetBytes($byte)
            } while ($byte[0] -ge $limite)
            $j = $byte[0] % ($i + 1)
            $temporal = $caracteres[$i]
            $caracteres[$i] = $caracteres[$j]
            $caracteres[$j] = $temporal
        }
        return -join $caracteres
    } finally {
        $aleatorio.Dispose()
        [Array]::Clear($byte, 0, $byte.Length)
    }
}

function Generar-Hash(
    [string]$contrasena,
    [string]$java,
    [string]$clases
) {
    $inicio = [Diagnostics.ProcessStartInfo]::new()
    $inicio.FileName = $java
    $inicio.UseShellExecute = $false
    $inicio.RedirectStandardInput = $true
    $inicio.RedirectStandardOutput = $true
    $inicio.RedirectStandardError = $true
    $inicio.CreateNoWindow = $true
    if ($inicio.PSObject.Properties.Name -contains 'ArgumentList') {
        $inicio.ArgumentList.Add('-cp')
        $inicio.ArgumentList.Add("$clases;$jarBCrypt")
        $inicio.ArgumentList.Add('GenerarHashBCrypt')
    } else {
        $inicio.Arguments = "-cp `"$clases;$jarBCrypt`" GenerarHashBCrypt"
    }

    $proceso = [Diagnostics.Process]::Start($inicio)
    $proceso.StandardInput.WriteLine($contrasena)
    $proceso.StandardInput.Close()
    $hash = $proceso.StandardOutput.ReadToEnd().Trim()
    $errorProceso = $proceso.StandardError.ReadToEnd().Trim()
    $proceso.WaitForExit()
    if ($proceso.ExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($hash)) {
        throw "No fue posible generar el hash BCrypt. $errorProceso"
    }
    return $hash
}

if (-not (Test-Path -LiteralPath $sql)) {
    throw 'No se encontro el SQL de la cuenta administrativa.'
}
if (-not (Test-Path -LiteralPath $jarBCrypt)) {
    throw 'No se encontro jBCrypt. Ejecuta mvn package antes de continuar.'
}
if (-not $jarPostgreSql) {
    throw 'No se encontro el controlador PostgreSQL en Maven local.'
}

$railway = Resolver-Railway
$java = Resolver-Java 'java'
$javac = Resolver-Java 'javac'
$estado = & $railway status --json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or -not $estado.id) {
    throw 'Railway CLI no esta vinculado al proyecto de SoraCare.'
}

$variables = & $railway variables --service Postgres `
    --environment production --json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) {
    throw 'No fue posible leer la conexion privada de PostgreSQL.'
}
foreach ($nombre in @('PGUSER', 'PGPASSWORD', 'PGDATABASE')) {
    if ([string]::IsNullOrWhiteSpace($variables.$nombre)) {
        throw "Railway no proporciono la variable $nombre."
    }
}

New-Item -ItemType Directory -Path $clasesTemporales -Force *> $null
& $javac -encoding UTF-8 -cp $jarBCrypt -d $clasesTemporales `
    $fuenteHash $fuenteJdbc
if ($LASTEXITCODE -ne 0) {
    throw 'No fue posible compilar el cargador seguro.'
}

$contrasena = Nueva-ContrasenaTemporal
$hash = Generar-Hash $contrasena $java $clasesTemporales
$puerto = Obtener-PuertoLibre
$salidaTunel = Join-Path $env:TEMP "soracare-adri-admin-$puerto.out.log"
$errorTunel = Join-Path $env:TEMP "soracare-adri-admin-$puerto.err.log"
$tunel = $null
try {
    $tunel = Start-Process -FilePath $railway -ArgumentList @(
        'connect', 'Postgres', '--environment', 'production',
        '--tunnel-only', '--port', "$puerto"
    ) -WindowStyle Hidden -RedirectStandardOutput $salidaTunel `
        -RedirectStandardError $errorTunel -PassThru
    Esperar-Puerto $puerto $tunel

    $inicio = [Diagnostics.ProcessStartInfo]::new()
    $inicio.FileName = $java
    $inicio.UseShellExecute = $false
    $inicio.RedirectStandardInput = $true
    $inicio.RedirectStandardOutput = $true
    $inicio.RedirectStandardError = $true
    $inicio.CreateNoWindow = $true
    $jdbcUrl = "jdbc:postgresql://127.0.0.1:$puerto/$($variables.PGDATABASE)"
    if ($inicio.PSObject.Properties.Name -contains 'ArgumentList') {
        $inicio.ArgumentList.Add('-cp')
        $inicio.ArgumentList.Add("$clasesTemporales;$jarPostgreSql")
        $inicio.ArgumentList.Add('EjecutarSqlJdbc')
        $inicio.ArgumentList.Add($jdbcUrl)
        $inicio.ArgumentList.Add($variables.PGUSER)
        $inicio.ArgumentList.Add($sql)
    } else {
        $inicio.Arguments =
            "-cp `"$clasesTemporales;$jarPostgreSql`" " +
            "EjecutarSqlJdbc `"$jdbcUrl`" `"$($variables.PGUSER)`" " +
            "`"$sql`""
    }

    $proceso = [Diagnostics.Process]::Start($inicio)
    foreach ($secreto in @(
        $variables.PGPASSWORD,
        $hash,
        $hash,
        $hash,
        $hash
    )) {
        $proceso.StandardInput.WriteLine($secreto)
    }
    $proceso.StandardInput.Close()
    $salida = $proceso.StandardOutput.ReadToEnd().Trim()
    $errorProceso = $proceso.StandardError.ReadToEnd().Trim()
    $proceso.WaitForExit()
    if ($proceso.ExitCode -ne 0) {
        throw "PostgreSQL rechazo la creacion de Adri Admin. $errorProceso"
    }

    $segura = ConvertTo-SecureString $contrasena -AsPlainText -Force
    [PSCustomObject]@{
        Usuario = 'adri.admin'
        Contrasena = $segura
        CreadaEn = [DateTimeOffset]::Now
        Entorno = 'Railway production'
    } | Export-Clixml -LiteralPath $archivoCredencial -Force

    try {
        Set-Clipboard -Value $contrasena
        $portapapeles = $true
    } catch {
        $portapapeles = $false
    }
} finally {
    if ($tunel -and -not $tunel.HasExited) {
        Stop-Process -Id $tunel.Id -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $salidaTunel, $errorTunel `
        -Force -ErrorAction SilentlyContinue
    $variables = $null
    $hash = $null
    $contrasena = $null
}

Write-Host 'Cuenta creada y verificada en PostgreSQL de Railway.' `
    -ForegroundColor Green
Write-Host 'Nombre visible: Adri Admin'
Write-Host 'Usuario: adri.admin'
Write-Host 'Rol: ADMINISTRADOR'
Write-Host 'Se solicitara cambiar la contrasena en el primer ingreso.'
if ($portapapeles) {
    Write-Host 'La contrasena temporal fue copiada al portapapeles.'
} else {
    Write-Host 'Usa scripts\copiar-credencial-adri-admin.ps1 para copiarla.'
}
