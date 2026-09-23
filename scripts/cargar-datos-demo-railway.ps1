param(
    [switch]$SinPaciente,
    [switch]$GenerarContrasenas
)

$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$sql = Join-Path $PSScriptRoot 'sql\datos-demo-docker.sql'
$fuenteHash = Join-Path $PSScriptRoot 'support\GenerarHashBCrypt.java'
$fuenteJdbc = Join-Path $PSScriptRoot 'support\EjecutarSqlJdbc.java'
$clasesTemporales = Join-Path $proyecto 'target\demo-seed-support'
$archivoCredenciales = Join-Path $proyecto '.env.demo-railway.clixml'
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
    throw 'No se encontró Railway CLI. Volvé a instalarlo o iniciá sesión.'
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
    throw "No se encontró $nombre. Instalá o configurá Java 21."
}

function Obtener-TextoSeguro([Security.SecureString]$seguro) {
    $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    }
}

function Leer-Contrasena([string]$descripcion) {
    while ($true) {
        $primera = Read-Host "Contraseña para $descripcion" -AsSecureString
        $segunda = Read-Host 'Repetí la contraseña' -AsSecureString
        try {
            $textoPrimero = Obtener-TextoSeguro $primera
            $textoSegundo = Obtener-TextoSeguro $segunda
            $errores = @()
            if ($textoPrimero.Length -lt 10 -or $textoPrimero.Length -gt 72) {
                $errores += 'debe tener entre 10 y 72 caracteres'
            }
            if ($textoPrimero -cnotmatch '[A-Z]') {
                $errores += 'debe incluir una mayúscula'
            }
            if ($textoPrimero -cnotmatch '[a-z]') {
                $errores += 'debe incluir una minúscula'
            }
            if ($textoPrimero -notmatch '\d') {
                $errores += 'debe incluir un número'
            }
            if ($textoPrimero -notmatch '[^A-Za-z0-9]') {
                $errores += 'debe incluir un carácter especial'
            }
            if ($textoPrimero -cne $textoSegundo) {
                $errores += 'las dos entradas no coinciden'
            }
            if ($errores.Count -eq 0) {
                return $primera.Copy()
            }
            Write-Host ('Contraseña inválida: ' + ($errores -join '; ') + '.') `
                -ForegroundColor Yellow
        } finally {
            $textoPrimero = $null
            $textoSegundo = $null
            $primera.Dispose()
            $segunda.Dispose()
        }
    }
}

function Nueva-ContrasenaDemo {
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
    } finally {
        $aleatorio.Dispose()
        [Array]::Clear($byte, 0, $byte.Length)
    }
    return ConvertTo-SecureString (-join $caracteres) -AsPlainText -Force
}

function Guardar-CredencialesDemo(
    [Security.SecureString]$contrasenaMedicos,
    [Security.SecureString]$contrasenaAdmin,
    [Security.SecureString]$contrasenaRecepcion,
    [Security.SecureString]$contrasenaPaciente,
    [string]$archivo
) {
    $registros = @(
        [PSCustomObject]@{
            Clave = 'medicos'
            Usuario = 'med.derma.valeria'
            Contrasena = $contrasenaMedicos.Copy()
        },
        [PSCustomObject]@{
            Clave = 'administracion'
            Usuario = 'prueba.seguridad'
            Contrasena = $contrasenaAdmin.Copy()
        },
        [PSCustomObject]@{
            Clave = 'recepcion'
            Usuario = 'prueba.recepcion'
            Contrasena = $contrasenaRecepcion.Copy()
        }
    )
    if ($contrasenaPaciente) {
        $registros += [PSCustomObject]@{
            Clave = 'paciente'
            Usuario = 'adrian.paciente'
            Contrasena = $contrasenaPaciente.Copy()
        }
    }
    try {
        $registros | Export-Clixml -LiteralPath $archivo -Force
    } finally {
        foreach ($registro in $registros) {
            $registro.Contrasena.Dispose()
        }
    }
}

function Generar-Hash(
    [Security.SecureString]$contrasena,
    [string]$java,
    [string]$directorioClases
) {
    $texto = Obtener-TextoSeguro $contrasena
    try {
        $inicio = [Diagnostics.ProcessStartInfo]::new()
        $inicio.FileName = $java
        $inicio.UseShellExecute = $false
        $inicio.RedirectStandardInput = $true
        $inicio.RedirectStandardOutput = $true
        $inicio.RedirectStandardError = $true
        $inicio.CreateNoWindow = $true
        if ($inicio.PSObject.Properties.Name -contains 'ArgumentList') {
            $inicio.ArgumentList.Add('-cp')
            $inicio.ArgumentList.Add("$directorioClases;$jarBCrypt")
            $inicio.ArgumentList.Add('GenerarHashBCrypt')
        } else {
            $inicio.Arguments =
                "-cp `"$directorioClases;$jarBCrypt`" GenerarHashBCrypt"
        }
        $proceso = [Diagnostics.Process]::Start($inicio)
        $proceso.StandardInput.WriteLine($texto)
        $proceso.StandardInput.Close()
        $salida = $proceso.StandardOutput.ReadToEnd().Trim()
        $errorProceso = $proceso.StandardError.ReadToEnd().Trim()
        $proceso.WaitForExit()
        if ($proceso.ExitCode -ne 0 -or $salida -notmatch '^\$2[aby]\$12\$') {
            throw "No fue posible generar el hash BCrypt. $errorProceso"
        }
        return $salida
    } finally {
        $texto = $null
        $contrasena.Dispose()
    }
}

function Obtener-PuertoLibre {
    $listener = [Net.Sockets.TcpListener]::new(
        [Net.IPAddress]::Loopback,
        0
    )
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
            throw 'Railway cerró el túnel antes de conectarse a PostgreSQL.'
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
    throw 'El túnel de Railway no estuvo listo dentro de 30 segundos.'
}

if (-not (Test-Path -LiteralPath $jarBCrypt)) {
    throw 'No se encontró jBCrypt. Ejecutá mvn package antes de continuar.'
}
if (-not $jarPostgreSql) {
    throw 'No se encontró el controlador PostgreSQL en el repositorio Maven local.'
}

$railway = Resolver-Railway
$java = Resolver-Java 'java'
$javac = Resolver-Java 'javac'

$estado = & $railway status --json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or -not $estado.id) {
    throw 'Railway CLI no está vinculado al proyecto de SoraCare.'
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

New-Item -ItemType Directory -Path $clasesTemporales -Force *> $null
& $javac -encoding UTF-8 -cp $jarBCrypt -d $clasesTemporales `
    $fuenteHash $fuenteJdbc
if ($LASTEXITCODE -ne 0) {
    throw 'No fue posible compilar el cargador seguro de datos.'
}

Write-Host ''
Write-Host 'Todos los médicos compartirán una contraseña solo en la demostración.' `
    -ForegroundColor Cyan
if ($GenerarContrasenas) {
    Write-Host 'Se generarán contraseñas aleatorias y se guardarán cifradas para este usuario de Windows.' `
        -ForegroundColor Cyan
    $contrasenaMedicos = Nueva-ContrasenaDemo
    $contrasenaAdmin = Nueva-ContrasenaDemo
    $contrasenaRecepcion = Nueva-ContrasenaDemo
    if (-not $SinPaciente) {
        $contrasenaPaciente = Nueva-ContrasenaDemo
    }
    Guardar-CredencialesDemo $contrasenaMedicos $contrasenaAdmin `
        $contrasenaRecepcion $contrasenaPaciente $archivoCredenciales
} else {
    Write-Host 'Se pedirán contraseñas. Los caracteres no se mostrarán.' `
        -ForegroundColor Cyan
    $contrasenaMedicos = Leer-Contrasena 'los 16 médicos de prueba'
    $contrasenaAdmin = Leer-Contrasena 'prueba.seguridad (administrador)'
    $contrasenaRecepcion = Leer-Contrasena 'prueba.recepcion'
}
Write-Host ''

$hashMedicos = Generar-Hash $contrasenaMedicos $java $clasesTemporales
$hashAdmin = Generar-Hash $contrasenaAdmin $java $clasesTemporales
$hashRecepcion = Generar-Hash $contrasenaRecepcion $java $clasesTemporales
if ($SinPaciente) {
    $hashPaciente = $hashRecepcion
} elseif ($GenerarContrasenas) {
    $hashPaciente = Generar-Hash $contrasenaPaciente $java $clasesTemporales
} else {
    $hashPaciente = Generar-Hash (Leer-Contrasena 'adrian.paciente') `
        $java $clasesTemporales
}

$sqlTemporal = $null
if ($SinPaciente) {
    $contenidoSql = [IO.File]::ReadAllText($sql)
    $contenidoSinPaciente = [Text.RegularExpressions.Regex]::Replace(
        $contenidoSql,
        "(?ms)^\s*\(920000003,.*?:'hash_paciente'\);\r?\n",
        ''
    )
    $contenidoSinPaciente = $contenidoSinPaciente.Replace(
        ":'hash_recepcion'),",
        ":'hash_recepcion');"
    ).Replace(
        'cantidad_cuentas <> 19',
        'cantidad_cuentas <> 18'
    )
    if ($contenidoSinPaciente -eq $contenidoSql `
        -or $contenidoSinPaciente.Contains("(920000003,")) {
        throw 'No fue posible preparar el SQL sin la cuenta paciente.'
    }
    $sqlTemporal = Join-Path $env:TEMP (
        'soracare-demo-railway-' + [Guid]::NewGuid().ToString('N') + '.sql'
    )
    [IO.File]::WriteAllText(
        $sqlTemporal,
        $contenidoSinPaciente,
        [Text.UTF8Encoding]::new($false)
    )
    $sql = $sqlTemporal
}

$puerto = Obtener-PuertoLibre
$salidaTunel = Join-Path $env:TEMP "soracare-railway-tunel-$puerto.out.log"
$errorTunel = Join-Path $env:TEMP "soracare-railway-tunel-$puerto.err.log"
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
    $jdbcUrl =
        "jdbc:postgresql://127.0.0.1:$puerto/$($variables.PGDATABASE)"
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
        $hashMedicos,
        $hashAdmin,
        $hashRecepcion,
        $hashPaciente
    )) {
        $proceso.StandardInput.WriteLine($secreto)
    }
    $proceso.StandardInput.Close()
    $salida = $proceso.StandardOutput.ReadToEnd().Trim()
    $errorProceso = $proceso.StandardError.ReadToEnd().Trim()
    $proceso.WaitForExit()
    if ($proceso.ExitCode -ne 0) {
        throw "PostgreSQL rechazó la carga demostrativa. $errorProceso"
    }
} finally {
    if ($tunel -and -not $tunel.HasExited) {
        Stop-Process -Id $tunel.Id -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $salidaTunel, $errorTunel `
        -Force -ErrorAction SilentlyContinue
    if ($sqlTemporal) {
        Remove-Item -LiteralPath $sqlTemporal `
            -Force -ErrorAction SilentlyContinue
    }
    $variables = $null
    $hashMedicos = $null
    $hashAdmin = $null
    $hashRecepcion = $null
    $hashPaciente = $null
}

Write-Host ''
Write-Host 'Datos ficticios cargados correctamente en Railway.' `
    -ForegroundColor Green
Write-Host 'Administrador: prueba.seguridad'
Write-Host 'Recepción: prueba.recepcion'
if (-not $SinPaciente) {
    Write-Host 'Paciente: adrian.paciente'
}
if ($GenerarContrasenas) {
    Write-Host ('Credenciales cifradas: ' + $archivoCredenciales)
    Write-Host 'Usá scripts\copiar-credencial-demo.ps1 para copiar una contraseña sin mostrarla.'
}
