param(
    [string]$BaseUrl = 'https://soracare-production.up.railway.app/SoraCare-FPUNE',
    [switch]$CrearFlujoDemostrativo
)

$ErrorActionPreference = 'Stop'
$proyecto = Split-Path -Parent $PSScriptRoot
$archivoCredenciales = Join-Path $proyecto '.env.demo-railway.clixml'

if (-not (Test-Path -LiteralPath $archivoCredenciales)) {
    throw 'No existen credenciales demostrativas cifradas en este equipo.'
}

function Obtener-EntradaHtml([string]$html, [string]$nombre) {
    $escapado = [Text.RegularExpressions.Regex]::Escape($nombre)
    $patron = '<input(?=[^>]*\bname\s*=\s*["'']' + $escapado +
        '["''])(?=[^>]*\bvalue\s*=\s*["'']([^"'']*)["''])[^>]*>'
    $coincidencia = [Text.RegularExpressions.Regex]::Match(
        $html,
        $patron,
        [Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if (-not $coincidencia.Success) {
        throw "No se encontró el campo $nombre en la respuesta HTML."
    }
    return [Net.WebUtility]::HtmlDecode($coincidencia.Groups[1].Value)
}

function Obtener-CredencialDemo([string]$clave) {
    $registro = Import-Clixml -LiteralPath $archivoCredenciales |
        Where-Object Clave -EQ $clave |
        Select-Object -First 1
    if (-not $registro -or -not $registro.Contrasena) {
        throw "No se encontró la cuenta cifrada $clave."
    }
    return [Management.Automation.PSCredential]::new(
        $registro.Usuario,
        $registro.Contrasena
    )
}

function Abrir-SesionDemo([string]$clave, [string]$rutaEsperada) {
    $sesion = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
    $pagina = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/login" `
        -WebSession $sesion -TimeoutSec 30
    $csrf = Obtener-EntradaHtml $pagina.Content 'csrfToken'
    $credencial = Obtener-CredencialDemo $clave
    $contrasena = $null
    try {
        $contrasena = $credencial.GetNetworkCredential().Password
        $respuesta = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/login" `
            -Method Post -WebSession $sesion -TimeoutSec 30 -Body @{
                csrfToken = $csrf
                identificador = $credencial.UserName
                contrasena = $contrasena
            }
    } finally {
        $contrasena = $null
    }
    $rutaFinal = $respuesta.BaseResponse.ResponseUri.AbsolutePath
    if (-not $rutaFinal.EndsWith($rutaEsperada)) {
        throw "La cuenta $clave terminó en una ruta inesperada: $rutaFinal"
    }
    Write-Host ("Acceso {0}: OK" -f $clave) -ForegroundColor Green
    return $sesion
}

function Comprobar-Pagina(
    [Microsoft.PowerShell.Commands.WebRequestSession]$sesion,
    [string]$ruta,
    [string]$textoEsperado
) {
    $respuesta = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + $ruta) `
        -WebSession $sesion -TimeoutSec 30
    $contenido = [Net.WebUtility]::HtmlDecode($respuesta.Content)
    if ($respuesta.StatusCode -ne 200 -or
        $contenido -notmatch [Text.RegularExpressions.Regex]::Escape($textoEsperado)) {
        throw "La ruta $ruta no mostró el contenido esperado."
    }
    Write-Host ("Ruta {0}: OK" -f $ruta) -ForegroundColor Green
    return $respuesta
}

function Proxima-FechaValeria {
    $fecha = [DateTime]::Today.AddDays(1)
    while ($fecha.DayOfWeek -notin @(
        [DayOfWeek]::Monday,
        [DayOfWeek]::Wednesday,
        [DayOfWeek]::Friday
    )) {
        $fecha = $fecha.AddDays(1)
    }
    return $fecha.ToString('yyyy-MM-dd')
}

function Crear-Y-Confirmar-CitaDemo(
    [Microsoft.PowerShell.Commands.WebRequestSession]$paciente,
    [Microsoft.PowerShell.Commands.WebRequestSession]$recepcion
) {
    $fecha = Proxima-FechaValeria
    $consulta = "$BaseUrl/mis-citas?nueva=1&idMedico=1&idEspecialidad=9" +
        "&fecha=$fecha&busqueda=turnos"
    $paginaReserva = Invoke-WebRequest -UseBasicParsing -Uri $consulta `
        -WebSession $paciente -TimeoutSec 30
    $slot = [Text.RegularExpressions.Regex]::Match(
        $paginaReserva.Content,
        '<option\s+value="([^"\r\n]*\|[^"\r\n]*)">',
        [Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if (-not $slot.Success) {
        throw "No existe un turno libre para la prueba del $fecha."
    }
    $valorSlot = [Net.WebUtility]::HtmlDecode($slot.Groups[1].Value)
    $csrf = Obtener-EntradaHtml $paginaReserva.Content 'csrfToken'
    $reserva = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/mis-citas" `
        -Method Post -WebSession $paciente -TimeoutSec 30 -Body @{
            csrfToken = $csrf
            fecha = $fecha
            nueva = '1'
            busqueda = 'turnos'
            idMedico = '1'
            idEspecialidad = '9'
            slot = $valorSlot
            motivoConsulta = 'Ensayo público de presentación'
            observaciones = 'Datos completamente ficticios.'
            metodoPago = 'EFECTIVO'
            tipoDocumentoFactura = 'CI'
            numeroDocumentoFactura = Obtener-EntradaHtml $paginaReserva.Content 'numeroDocumentoFactura'
            nombreFactura = Obtener-EntradaHtml $paginaReserva.Content 'nombreFactura'
            correoFactura = Obtener-EntradaHtml $paginaReserva.Content 'correoFactura'
            direccionFactura = Obtener-EntradaHtml $paginaReserva.Content 'direccionFactura'
        }
    if ($reserva.Content -notmatch 'pago en efectivo sigue pendiente') {
        throw 'La cita ficticia no quedó reservada con pago pendiente.'
    }
    Write-Host 'Reserva ficticia con pago en efectivo: OK' -ForegroundColor Green

    $agenda = Invoke-WebRequest -UseBasicParsing `
        -Uri "$BaseUrl/agenda/pagos?cedula=920000003" `
        -WebSession $recepcion -TimeoutSec 30
    $pago = [Text.RegularExpressions.Regex]::Match(
        $agenda.Content,
        '(?s)name="accion"\s+value="confirmarEfectivo".*?name="idPago"\s+value="(\d+)"',
        [Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if (-not $pago.Success) {
        throw 'Recepción no encontró el pago pendiente del paciente ficticio.'
    }
    $csrfRecepcion = Obtener-EntradaHtml $agenda.Content 'csrfToken'
    $confirmacion = Invoke-WebRequest -UseBasicParsing `
        -Uri "$BaseUrl/agenda/pagos" -Method Post `
        -WebSession $recepcion -TimeoutSec 30 -Body @{
            csrfToken = $csrfRecepcion
            accion = 'confirmarEfectivo'
            idPago = $pago.Groups[1].Value
            cedula = '920000003'
        }
    $contenidoConfirmacion = [Net.WebUtility]::HtmlDecode(
        $confirmacion.Content
    )
    if ($contenidoConfirmacion -notmatch 'comprobante qued') {
        throw 'Recepción no confirmó el pago ficticio.'
    }
    Write-Host 'Confirmación del cobro ficticio: OK' -ForegroundColor Green

    $enlacePdf = [Text.RegularExpressions.Regex]::Match(
        $confirmacion.Content,
        '/SoraCare-FPUNE/comprobantes/pago\?id=(\d+)',
        [Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if (-not $enlacePdf.Success) {
        throw 'El pago confirmado no mostró un enlace al comprobante PDF.'
    }
    $temporal = Join-Path $env:TEMP (
        'soracare-comprobante-' + [Guid]::NewGuid().ToString('N') + '.pdf'
    )
    try {
        Invoke-WebRequest -UseBasicParsing `
            -Uri ("$BaseUrl/comprobantes/pago?id=" + $enlacePdf.Groups[1].Value) `
            -WebSession $recepcion -TimeoutSec 30 -OutFile $temporal
        $bytes = [IO.File]::ReadAllBytes($temporal)
        if ($bytes.Length -lt 1000 -or
            [Text.Encoding]::ASCII.GetString($bytes, 0, 4) -ne '%PDF') {
            throw 'El comprobante descargado no es un PDF válido.'
        }
    } finally {
        Remove-Item -LiteralPath $temporal -Force -ErrorAction SilentlyContinue
    }
    Write-Host 'Comprobante PDF autenticado: OK' -ForegroundColor Green
}

$inicio = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/login" -TimeoutSec 30
if ($inicio.Content -notmatch 'accounts\.google\.com/gsi/client') {
    throw 'El acceso con Google no está publicado en Railway.'
}
Write-Host 'Google Identity visible: OK' -ForegroundColor Green

$sesionPaciente = Abrir-SesionDemo 'paciente' '/paciente/inicio'
[void](Comprobar-Pagina $sesionPaciente '/mis-citas' 'Mis citas')
[void](Comprobar-Pagina $sesionPaciente '/mis-recetas' 'indicaciones')
[void](Comprobar-Pagina $sesionPaciente '/mis-documentos' 'documentos')

$sesionRecepcion = Abrir-SesionDemo 'recepcion' '/recepcion/inicio'
[void](Comprobar-Pagina $sesionRecepcion '/agenda/pagos' 'Pagos y comprobantes')

$sesionMedico = Abrir-SesionDemo 'medicos' '/medico/inicio'
[void](Comprobar-Pagina $sesionMedico '/medico/agenda' 'Agenda')
[void](Comprobar-Pagina $sesionMedico '/medico/facturas' 'Facturaci')

$sesionAdmin = Abrir-SesionDemo 'administracion' '/admin/inicio'
[void](Comprobar-Pagina $sesionAdmin '/admin/informes' 'Informes')
[void](Comprobar-Pagina $sesionAdmin '/admin/estado-sistema' 'Estado del sistema')

if ($CrearFlujoDemostrativo) {
    Crear-Y-Confirmar-CitaDemo $sesionPaciente $sesionRecepcion
}

Write-Host ''
Write-Host 'Verificación pública de presentación completada.' -ForegroundColor Green
