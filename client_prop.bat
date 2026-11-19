@echo off
rem Executa o cliente MCP passando o diretório autorizado como
rem propriedade da JVM (-Dmcp.files.root).
rem Uso: client_prop.bat DIRETORIO_AUTORIZADO
setlocal

if "%~1"=="" (
   echo Uso: %~nx0 DIRETORIO_AUTORIZADO
   exit /b 1
)

cd /d "%~dp0target\dist-client"
java -Dmcp.files.root="%~1" -jar mcp_server01_client.jar

endlocal
