$ErrorActionPreference = 'Stop'

$proyecto = Split-Path -Parent $PSScriptRoot
$compose = Join-Path $proyecto 'compose.yaml'
$entorno = Join-Path $proyecto '.env.docker'
$sql = Join-Path $PSScriptRoot 'sql\datos-demo-docker.sql'
$fuenteHash = Join-Path $PSScriptRoot 'support\GenerarHashBCrypt.java'
$clasesTemporales = Join-Path $proyecto 'target\demo-seed-support'
$jarBCrypt = Join-Path $env:USERPROFILE `
    '.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar'

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
    throw 'Docker no está instalado o no puede encontrarse.'
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

if (-not (Test-Path -LiteralPath $entorno)) {
    throw 'Primero ejecutá scripts\iniciar-docker.ps1 para crear .env.docker.'
}
if (-not (Test-Path -LiteralPath $jarBCrypt)) {
    throw 'No se encontró jBCrypt en el repositorio local de Maven. Ejecutá mvn package.'
}

$docker = Resolver-Docker
$java = Resolver-Java 'java'
$javac = Resolver-Java 'javac'

& $docker info --format '{{.ServerVersion}}' *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop está instalado, pero su motor no está activo.'
}

$contenedorPostgres = & $docker compose --env-file $entorno -f $compose `
    ps -q postgres
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($contenedorPostgres)) {
    throw 'PostgreSQL de SoraCare no está iniciado. Ejecutá scripts\iniciar-docker.ps1.'
}

New-Item -ItemType Directory -Path $clasesTemporales -Force *> $null
& $javac -encoding UTF-8 -cp $jarBCrypt -d $clasesTemporales $fuenteHash
if ($LASTEXITCODE -ne 0) {
    throw 'No fue posible compilar el generador seguro de hashes.'
}

Write-Host ''
Write-Host 'Se pedirán cuatro contraseñas. Los caracteres no se mostrarán.' `
    -ForegroundColor Cyan
Write-Host 'Todos los médicos compartirán una contraseña solamente en este entorno de prueba.' `
    -ForegroundColor Cyan
Write-Host ''

$hashMedicos = Generar-Hash (Leer-Contrasena 'los 16 médicos de prueba') `
    $java $clasesTemporales
$hashAdmin = Generar-Hash (Leer-Contrasena 'prueba.seguridad (administrador)') `
    $java $clasesTemporales
$hashRecepcion = Generar-Hash (Leer-Contrasena 'prueba.recepcion') `
    $java $clasesTemporales
$hashPaciente = Generar-Hash (Leer-Contrasena 'adrian.paciente') `
    $java $clasesTemporales

$rutaTemporal = '/tmp/soracare-datos-demo.sql'
& $docker cp $sql "${contenedorPostgres}:$rutaTemporal"
if ($LASTEXITCODE -ne 0) {
    throw 'No fue posible copiar el cargador SQL al contenedor.'
}

try {
    & $docker compose --env-file $entorno -f $compose exec -T postgres `
        psql -U postgres -d soracare_db `
        --set=ON_ERROR_STOP=1 `
        --set="hash_medicos=$hashMedicos" `
        --set="hash_admin=$hashAdmin" `
        --set="hash_recepcion=$hashRecepcion" `
        --set="hash_paciente=$hashPaciente" `
        --file=$rutaTemporal
    if ($LASTEXITCODE -ne 0) {
        throw 'PostgreSQL rechazó la carga; la transacción fue revertida.'
    }
} finally {
    & $docker exec $contenedorPostgres rm -f $rutaTemporal *> $null
    $hashMedicos = $null
    $hashAdmin = $null
    $hashRecepcion = $null
    $hashPaciente = $null
}

Write-Host ''
Write-Host 'Datos demostrativos cargados correctamente.' -ForegroundColor Green
Write-Host 'Administrador: prueba.seguridad'
Write-Host 'Recepción: prueba.recepcion'
Write-Host 'Paciente: adrian.paciente'
Write-Host 'Los usuarios de médicos se mostrarán en la validación final.'
