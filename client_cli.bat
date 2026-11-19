@echo off
rem Executa o cliente MCP passando o diretório autorizado como argumento.
rem Uso: client_cli.bat DIRETORIO_AUTORIZADO
setlocal

if "%~1"=="" (
   echo Uso: %~nx0 DIRETORIO_AUTORIZADO
   exit /b 1
)

cd /d "%~dp0target\dist-client"
java -jar mcp_server01_client.jar "%~1"

endlocal
