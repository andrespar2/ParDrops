# ============================================================================
# ParDrops - script de desarrollo
# ----------------------------------------------------------------------------
# Abre dos ventanas: el backend (Spring Boot en http://localhost:8080) y el
# frontend (Angular en http://localhost:4200).
#
# Uso:  .\dev.ps1
#
# Usa el JDK portátil incluido en .tools\jdk25, por lo que no hace falta
# configurar JAVA_HOME manualmente. Para detener todo, cierra las ventanas.
# ============================================================================

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$javaHome = Join-Path $root '.tools\jdk25'
$backendDir = Join-Path $root 'pardrops'
$frontendDir = Join-Path $root 'pardropsfrontend'

if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
  throw "No se encontró el JDK en '$javaHome'. Descarga un JDK 25 y descomprímelo ahí."
}
if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
  Write-Host 'Instalando dependencias del frontend (solo la primera vez)...'
  npm install --prefix $frontendDir
}

Write-Host 'Arrancando backend  ->  http://localhost:8080'
Start-Process powershell -ArgumentList @(
  '-NoExit', '-Command', "`$env:JAVA_HOME='$javaHome'; cd '$backendDir'; .\mvnw.cmd spring-boot:run"
)

Start-Sleep -Seconds 8

Write-Host 'Arrancando frontend ->  http://localhost:4200'
Start-Process powershell -ArgumentList @(
  '-NoExit', '-Command', "cd '$frontendDir'; npm start"
)

Write-Host ''
Write-Host 'Listo. Espera a que el backend termine de arrancar y abre:'
Write-Host '  http://localhost:4200'
Write-Host ''
Write-Host 'Cuenta de administrador: admin@pardrops.com / admin123'
