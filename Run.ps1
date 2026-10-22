param([switch]$Check)
$ErrorActionPreference="Stop"
Set-Location -LiteralPath $PSScriptRoot
$jdkDir=Join-Path $env:USERPROFILE ".jdks/openjdk-25.0.1"
if($env:JAVA_HOME){$jdkDir=$env:JAVA_HOME}
$compiler=Join-Path $jdkDir "bin/javac.exe"
$runtime=Join-Path $jdkDir "bin/java.exe"
if(!(Test-Path -LiteralPath $compiler)){
    $compiler=(Get-Command javac -ErrorAction Stop).Source
    $runtime=(Get-Command java -ErrorAction Stop).Source
}
$env:PATH=(Split-Path $compiler)+";"+$env:PATH
New-Item -ItemType Directory -Force .build/Java | Out-Null
$sources=Get-ChildItem -LiteralPath src/Java -Filter *.java | ForEach-Object FullName
& $compiler --release 17 -encoding UTF-8 -d .build/Java @sources
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
if($Check){
    & python tools/run.py --check
}else{
    & $runtime -Xmx384m -cp .build/Java Main
}
exit $LASTEXITCODE
