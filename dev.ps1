# ============================================================================
# ParDrops - script de desarrollo
# ----------------------------------------------------------------------------
# Abre dos ventanas: el backend (Spring Boot en http://localhost:8080) y el
# frontend (Angular en http://localhost:4200).
#
# Uso:  .\dev.ps1
#
# Para detener todo, cierra las ventanas.
#
# Requisitos: Node.js 20+ y un JDK 25. El JDK se resuelve en este orden:
#   1) .tools\jdk25  (JDK portátil, no versionado)
#   2) variable de entorno JAVA_HOME
#   3) `java` disponible en el PATH
# Si no hay ninguno, se avisa y se detiene.
# ============================================================================

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$backendDir = Join-Path $root 'pardrops'
$frontendDir = Join-Path $root 'pardropsfrontend'

# Carga las variables locales del proyecto (por ejemplo, las credenciales de
# Google). El archivo .env.local no se versiona y es opcional.
$envLocal = Join-Path $root '.env.local'
if (Test-Path $envLocal) {
  Get-Content $envLocal | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim().Trim('"').Trim("'")
  }
  Write-Host 'Variables locales cargadas desde .env.local'
}

function Test-JavaHome([string]$path) {
  return $path -and (Test-Path (Join-Path $path 'bin\java.exe'))
}

# 1) JDK portátil incluido
$javaHome = Join-Path $root '.tools\jdk25'
if (-not (Test-JavaHome $javaHome)) {
  # 2) JAVA_HOME del sistema
  $javaHome = $env:JAVA_HOME
  if (-not (Test-JavaHome $javaHome)) {
    # 3) `java` en el PATH: derivar el home desde la ruta del ejecutable
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd -and $javaCmd.Source) {
      $javaHome = Split-Path (Split-Path $javaCmd.Source -Parent) -Parent
    }
  }
}

if (-not (Test-JavaHome $javaHome)) {
  throw "No se encontró ningún JDK 25. Instala un JDK 25 y define JAVA_HOME, o colócalo en .tools\jdk25."
}
Write-Host "Usando JDK: $javaHome"
$env:JAVA_HOME = $javaHome

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
