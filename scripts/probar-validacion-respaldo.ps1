$ErrorActionPreference = 'Stop'
# Datos sinteticos: prueba que se rechace la corrupcion antes de crear contenedores.
$raizTemporal = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$prueba = Join-Path $raizTemporal ('soracare-test-respaldo-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $prueba | Out-Null
try {
    Set-Content -LiteralPath (Join-Path $prueba 'base.dump') -Value 'datos sinteticos'
    Set-Content -LiteralPath (Join-Path $prueba 'archivos.tar.gz') -Value 'datos sinteticos'
    $archivos = @('base.dump', 'archivos.tar.gz') | ForEach-Object {
        $ruta = Join-Path $prueba $_
        @{ nombre = $_; bytes = (Get-Item $ruta).Length; sha256 = (Get-FileHash $ruta).Hash }
    }
    @{ formato = 1; archivos = @($archivos) } | ConvertTo-Json -Depth 4 |
        Set-Content -LiteralPath (Join-Path $prueba 'manifiesto.json') -Encoding UTF8
    Add-Content -LiteralPath (Join-Path $prueba 'base.dump') -Value 'alterado'
    $rechazado = $false
    try { & (Join-Path $PSScriptRoot 'verificar-respaldo-docker.ps1') -Ruta $prueba }
    catch {
        if ($_.Exception.Message -notlike 'Respaldo incompleto o alterado:*') { throw }
        $rechazado = $true
    }
    if (-not $rechazado) { throw 'La copia corrupta no fue rechazada.' }
    Write-Host 'OK: copia corrupta rechazada antes de acceder a Docker.'
} finally {
    $resuelta = [IO.Path]::GetFullPath($prueba)
    if ((Split-Path -Parent $resuelta).TrimEnd('\') -ne $raizTemporal.TrimEnd('\') -or
        (Split-Path -Leaf $resuelta) -notlike 'soracare-test-respaldo-*') { throw 'Ruta temporal inesperada.' }
    Remove-Item -LiteralPath $resuelta -Recurse -Force
}
